package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.avsluttbehandling.AvsluttBehandlingTask
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask
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
) {
    @Transactional
    fun revurderFagsak(fagsakId: Long): Behandling {
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
        val opprettetVedtak = autovedtakService.opprettTotrinnskontrollForAutomatiskBehandling(behandlingEtterBehandlingsresultat)

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

        return behandlingEtterBehandlingsresultat
    }
}
