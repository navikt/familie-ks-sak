package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
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
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun revurderFagsak(
        fagsakId: Long,
        erFremtidigOpphør: Boolean = false,
    ): Behandling? =
        if (behandlingRepository.finnBehandlinger(fagsakId).any { it.opprettetÅrsak == BehandlingÅrsak.LOVENDRING_2024 }) {
            logger.info("Lovendring 2024 allerede kjørt for fagsakId=$fagsakId")
            null
        } else {
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

            val erLovendringOgFremtidigOpphørOgHarFlereAndeler = behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(behandlingEtterBehandlingsresultat)

            if (behandlingEtterBehandlingsresultat.skalSendeVedtaksbrev(erLovendringOgFremtidigOpphørOgHarFlereAndeler)) {
                stegService.utførSteg(behandlingId = behandlingEtterBehandlingsresultat.id, behandlingSteg = BehandlingSteg.SIMULERING)
                stegService.utførSteg(behandlingId = behandlingEtterBehandlingsresultat.id, behandlingSteg = BehandlingSteg.VEDTAK)
            } else {
                autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandlingEtterBehandlingsresultat)
            }

            val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId = behandlingEtterBehandlingsresultat.id)

            taskService.save(
                IverksettMotOppdragTask.opprettTask(
                    behandling = behandlingEtterBehandlingsresultat,
                    vedtakId = vedtak.id,
                    saksbehandlerId = SikkerhetContext.SYSTEM_FORKORTELSE,
                ),
            )
            behandlingEtterBehandlingsresultat
        }
}
