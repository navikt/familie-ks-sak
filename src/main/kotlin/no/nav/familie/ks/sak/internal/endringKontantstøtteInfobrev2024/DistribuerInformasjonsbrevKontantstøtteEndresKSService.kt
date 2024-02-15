package no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024

import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DistribuerInformasjonsbrevKontantstøtteEndresKSService(
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringKS(erDryRun: Boolean): List<Long> {
        val fagsakerMedBarnFødtEtterAugust2022 = fagsakRepository.hentLøpendeFagsakerMedBarnFødtEtterAugust2022()

        if (!erDryRun) {
            fagsakerMedBarnFødtEtterAugust2022.forEach {
                logger.info("Oppretter task for å journalføre og distribuere informasjonsbrev om kontantstøtteendring på fagsak $it")
                val task = SendInformasjonsbrevKontantstøtteendringTask.lagTask(fagsakId = it)

                taskService.save(task)
            }
        }

        return fagsakerMedBarnFødtEtterAugust2022
    }
}
