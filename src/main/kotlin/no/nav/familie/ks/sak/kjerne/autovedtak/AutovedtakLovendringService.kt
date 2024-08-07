package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakLovendringService(
    private val fagsakService: FagsakService,
    private val behandlingRepository: BehandlingRepository,
    private val snikeIKøenService: SnikeIKøenService,
    private val taskService: TaskService,
    private val autovedtakService: AutovedtakService,
    private val stegService: StegService,
    private val vedtakRepository: VedtakRepository,
) {
    @Transactional
    fun revurderFagsak(fagsakId: Long): Behandling {
        // TODO gjør tjenesten idempotent
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
        val behandlingEtterBehandlingsresultat = autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(aktør = søkerAktør, behandlingÅrsak = BehandlingÅrsak.LOVENDRING_2024, behandlingType = BehandlingType.REVURDERING)

        if (behandlingEtterBehandlingsresultat.skalSendeVedtaksbrev()) {
            stegService.utførSteg(behandlingId = behandlingEtterBehandlingsresultat.id, behandlingSteg = BehandlingSteg.SIMULERING)
        }

        stegService.utførSteg(behandlingId = behandlingEtterBehandlingsresultat.id, behandlingSteg = BehandlingSteg.VEDTAK)

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingEtterBehandlingsresultat.id)

        taskService.save(
            IverksettMotOppdragTask.opprettTask(
                behandling = behandlingEtterBehandlingsresultat,
                vedtakId = vedtak.id,
                saksbehandlerId = SikkerhetContext.SYSTEM_FORKORTELSE,
            ),
        )

        return behandlingEtterBehandlingsresultat
    }
}
