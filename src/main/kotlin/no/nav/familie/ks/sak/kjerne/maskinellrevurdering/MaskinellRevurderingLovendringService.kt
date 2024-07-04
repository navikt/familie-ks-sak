package no.nav.familie.ks.sak.kjerne.maskinellrevurdering

import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.avsluttbehandling.AvsluttBehandlingTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MaskinellRevurderingLovendringService(
    private val stegService: StegService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val snikeIKøenService: SnikeIKøenService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
) {
    @Transactional
    fun revurderFagsak(fagsakId: Long) {
        val fagsak = fagsakService.hentFagsak(fagsakId = fagsakId)

        val aktivOgÅpenBehandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = fagsakId)

        if (aktivOgÅpenBehandling != null) {
            if (snikeIKøenService.kanSnikeForbi(aktivOgÅpenBehandling)) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.LOVENDRING,
                )
            } else {
                throw Feil("Kan ikke gjennomføre revurderingsbehandling for fagsak=$fagsakId fordi det er en åpen behandling vi ikke klarer å snike forbi")
            }
        }

        val søkerAktør = fagsak.aktør
        val behandlingEtterBehandlingsresultat = opprettMaskinellRevurderingOgKjørTilBehandlingsresultat(aktør = søkerAktør)
        val opprettetVedtak = opprettTotrinnskontrollOgVedtaksbrevForMaskinellRevurdering(behandlingEtterBehandlingsresultat)

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                BehandlingSteg.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettetVedtak.id,
                        SikkerhetContext.SYSTEM_FORKORTELSE,
                    )
                }

                BehandlingSteg.AVSLUTT_BEHANDLING -> {
                    AvsluttBehandlingTask.opprettTask(
                        søkerAktør.aktivFødselsnummer(),
                        behandlingEtterBehandlingsresultat.id,
                    )
                }

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved månedlig valutajustering for fagsak=$fagsakId")
            }

        taskService.save(task)
    }

    private fun opprettMaskinellRevurderingOgKjørTilBehandlingsresultat(
        aktør: Aktør,
    ): Behandling {
        val behandling =
            opprettBehandlingService.opprettBehandling(
                opprettBehandlingRequest =
                    OpprettBehandlingDto(
                        søkersIdent = aktør.aktivFødselsnummer(),
                        behandlingType = BehandlingType.REVURDERING,
                        behandlingÅrsak = BehandlingÅrsak.LOVENDRING_2024,
                    ),
            )

        stegService.utførSteg(behandling.id, BehandlingSteg.VILKÅRSVURDERING)
        stegService.utførSteg(behandling.id, BehandlingSteg.BEHANDLINGSRESULTAT)

        return behandlingService.hentBehandling(behandling.id)
    }

    private fun opprettTotrinnskontrollOgVedtaksbrevForMaskinellRevurdering(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId = behandling.id)
        return vedtakService.oppdaterVedtak(vedtak = vedtak)
    }
}
