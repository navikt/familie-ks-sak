package no.nav.familie.ks.sak.internal.EndringKontantstøtte2024

import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DistribuerInformasjonsbrevKontantsøtteEndresKSService(
    private val fagsakRepository: FagsakRepository,
    private val taskService: TaskService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevKontantstøtteendringKS(erDryRun: Boolean): List<Long> {
        val fagsakerMedBarnFødtI2023EllerSenere = fagsakRepository.hentLøpendeFagsakerMedBarnFødtI2023EllerSenere()

        if (!erDryRun) {
            val taskerForSakerIKs =
                fagsakerMedBarnFødtI2023EllerSenere.map {
                    logger.info("Oppretter task for å journalføre og distribuere informasjonsbrev om kontantstøtteendring på fagsak $it")
                    SendInformasjonsbrevKontantstøtteendringTask.lagTask(it)
                }
            taskService.saveAll(taskerForSakerIKs)
        }

        return fagsakerMedBarnFødtI2023EllerSenere
    }
}
