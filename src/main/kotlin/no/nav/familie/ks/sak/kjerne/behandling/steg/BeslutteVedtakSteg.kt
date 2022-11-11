package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.FeatureToggleConfig
import no.nav.familie.ks.sak.config.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.oppgave.FerdigstillOppgaverTask
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BeslutteVedtakSteg(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val loggService: LoggService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val featureToggleService: FeatureToggleService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK

    @Transactional
    override fun utførSteg(behandlingId: Long, behandlingStegDto: BehandlingStegDto) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val behandling = behandlingService.hentBehandling(behandlingId)

        validerAtBehandlingKanBesluttes(behandling)

        val besluttVedtakDto = behandlingStegDto as BesluttVedtakDto

        val totrinnskontroll = totrinnskontrollService.besluttTotrinnskontroll(
            behandlingId = behandling.id,
            beslutter = SikkerhetContext.hentSaksbehandlerNavn(),
            beslutterId = SikkerhetContext.hentSaksbehandler(),
            beslutning = besluttVedtakDto.beslutning,
            kontrollerteSider = besluttVedtakDto.kontrollerteSider
        )

        if (besluttVedtakDto.beslutning.erGodkjent()) {
            opprettTaskFerdigstillGodkjenneVedtak(
                behandling = behandling,
                beslutning = besluttVedtakDto.beslutning,
                begrunnelse = behandlingStegDto.begrunnelse
            )
        } else {
            val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId)

            vilkårsvurderingService.oppdater(vilkårsvurdering)

            vedtakService.opprettOgInitierNyttVedtakForBehandling(
                behandling = behandling,
                kopierVedtakBegrunnelser = true
            )

            opprettTaskFerdigstillGodkjenneVedtak(
                behandling = behandling,
                beslutning = behandlingStegDto.beslutning,
                begrunnelse = behandlingStegDto.begrunnelse
            )

            val behandleUnderkjentVedtakTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                tilordnetRessurs = totrinnskontroll.saksbehandlerId,
                fristForFerdigstillelse = LocalDate.now()
            )

            taskService.save(behandleUnderkjentVedtakTask)
        }
    }

    private fun validerAtBehandlingKanBesluttes(behandling: Behandling) {
        when {
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ->
                throw FunksjonellFeil("Behandlingen er allerede sendt til oppdrag og venter på kvittering")

            behandling.status == BehandlingStatus.AVSLUTTET ->
                throw FunksjonellFeil("Behandlingen er allerede avsluttet")

            behandling.opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV &&
                !featureToggleService.isEnabled(FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV) ->
                throw FunksjonellFeil(
                    melding = "Årsak ${BehandlingÅrsak.KORREKSJON_VEDTAKSBREV.visningsnavn} og toggle ${FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV} false",
                    frontendFeilmelding = "Du har ikke tilgang til å beslutte for denne behandlingen. Ta kontakt med teamet dersom dette ikke stemmer."
                )
        }
    }

    private fun opprettTaskFerdigstillGodkjenneVedtak(
        behandling: Behandling,
        beslutning: Beslutning,
        begrunnelse: String?
    ) {
        loggService.opprettBeslutningOmVedtakLogg(behandling, beslutning, begrunnelse)

        val ferdigstillGodkjenneVedtakTask =
            FerdigstillOppgaverTask.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)

        taskService.save(ferdigstillGodkjenneVedtakTask)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BeslutteVedtakSteg::class.java)
    }
}
