package no.nav.familie.ks.sak.internal.kontantstøtteInfobrevJuli2024

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DistribuerInformasjonsbrevKontantstøtteJuli2024(
    private val behandlingRepository: BehandlingRepository,
    private val taskService: TaskService,
) {
    private val logger: Logger = LoggerFactory.getLogger(DistribuerInformasjonsbrevKontantstøtteJuli2024::class.java)

    @Transactional
    fun hentAlleFagsakIdSomDetSkalSendesBrevTil(): List<Long> =
        behandlingRepository
            .finnBehandlingerSomSkalMottaInformasjonsbrevOmKontantstøtteLovendringJuli2024()
            .map { it.fagsak.id }
            .distinct()

    @Transactional
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøttJuli2024() {
        val fagsakerSomHarBehandlingSomSkalMottaInformasjonsbrevOmKontantstøtte =
            behandlingRepository
                .finnBehandlingerSomSkalMottaInformasjonsbrevOmKontantstøtteLovendringJuli2024()
                .map { it.fagsak }
                .distinct()

        fagsakerSomHarBehandlingSomSkalMottaInformasjonsbrevOmKontantstøtte.forEach {
            logger.info("Oppretter task for å journalføre og distribuere informasjonsbrev om kontantstøtteendring juli 2024 på fagsak $it")
            val task = SendInformasjonsbrevKontantstøtteendringTask.lagTask(fagsakId = it.id)

            taskService.save(task)
        }
    }
}
