package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.api.dto.HenleggÅrsak
import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HenleggBehandlingService(
    private val stegService: StegService,
    private val featureToggleService: FeatureToggleService,
    private val brevService: BrevService,
    private val oppgaveService: OppgaveService,
    private val loggService: LoggService,
    private val fagsakService: FagsakService,
    private val sakStatistikkService: SakStatistikkService,
    private val behandlingRepository: BehandlingRepository,
) {
    @Transactional
    fun henleggBehandling(
        behandlingId: Long,
        henleggÅrsak: HenleggÅrsak,
        begrunnelse: String,
    ) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        validerOmBehandlingKanHenlegges(behandling, henleggÅrsak)

        // send henleggelse brev
        if (henleggÅrsak == HenleggÅrsak.SØKNAD_TRUKKET) {
            brevService.genererOgSendBrev(
                behandlingId = behandling.id,
                manueltBrevDto =
                    ManueltBrevDto(
                        brevmal = Brevmal.HENLEGGE_TRUKKET_SØKNAD,
                        mottakerIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                    ),
            )
        }

        // ferdigstille oppgaver
        oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling).forEach {
            oppgaveService.ferdigstillOppgaver(behandling, it.type)
        }

        // lag historikkinnslag
        loggService.opprettHenleggBehandlingLogg(
            behandling = behandling,
            årsak = henleggÅrsak.beskrivelse,
            begrunnelse = begrunnelse,
        )

        // henlegg behandling steg
        stegService.settAlleStegTilAvbrutt(behandling)

        // muterer behandling resultat og status
        behandling.resultat = henleggÅrsak.tilBehandlingsresultat()
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.aktiv = false

        // oppdater fagsak status til avsluttet hvis første behandling på fagsak er henlagt
        val alleBehandlinger = behandlingRepository.finnBehandlinger(behandling.fagsak.id)
        when (alleBehandlinger.size) {
            1 -> {
                fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET)
            }

            else -> {
                // aktiverer siste vedtatt behandling på nytt
                alleBehandlinger
                    .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
                    .maxByOrNull { it.aktivertTidspunkt }
                    ?.apply {
                        aktiv = true
                        behandlingRepository.saveAndFlush(this)
                    }
            }
        }

        // Send data til Dvh
        sakStatistikkService.opprettSendingAvBehandlingensTilstand(behandlingId, behandling.steg)

        // trenger ikke å kalle eksplisitt save fordi behandling objekt er mutert og ha @Transactional
    }

    private fun validerOmBehandlingKanHenlegges(
        behandling: Behandling,
        henleggÅrsak: HenleggÅrsak,
    ) {
        val behandlingId = behandling.id
        when {
            HenleggÅrsak.TEKNISK_VEDLIKEHOLD == henleggÅrsak &&
                !featureToggleService.isEnabled(FeatureToggle.TEKNISK_VEDLIKEHOLD_HENLEGGELSE) -> {
                throw Feil(
                    "Teknisk vedlikehold henleggele er ikke påslått for " +
                        "${SikkerhetContext.hentSaksbehandlerNavn()}. Kan ikke henlegge behandling $behandlingId.",
                )
            }

            behandling.erAvsluttet() -> {
                throw Feil("Behandling $behandlingId er allerede avsluttet. Kan ikke henlegge behandling.")
            }

            // Hvis behandling kan behandles, kan den henlegges
            henleggÅrsak != HenleggÅrsak.TEKNISK_VEDLIKEHOLD && !behandling.steg.kanStegBehandles() -> {
                throw FunksjonellFeil(
                    "Behandling $behandlingId er på steg ${behandling.steg.visningsnavn()} " +
                        "og er da låst for alle andre type endringer. Kan ikke henlegge behandling.",
                )
            }

            behandling.erTekniskEndring() && !featureToggleService.isEnabled(FeatureToggle.TEKNISK_ENDRING) -> {
                throw FunksjonellFeil(
                    "Du har ikke tilgang til å henlegge en behandling " +
                        "som er opprettet med årsak=${behandling.opprettetÅrsak.visningsnavn}. " +
                        "Ta kontakt med teamet dersom dette ikke stemmer.",
                )
            }
        }
    }
}
