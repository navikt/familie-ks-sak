package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.task.FinnFagsakerSomSkalLåsesTask
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(
    private val taskService: TaskRepositoryWrapper,
    private val envService: EnvService,
    private val featureToggleService: FeatureToggleService,
) {
    /*
     * Siden kontantstøtte er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "\${CRON_FAGSAKSTATUS_SCHEDULER}")
    fun oppdaterFagsakStatuser() {
        val erLederpodEllerLokal = envService.erLokal() || LeaderClient.isLeader() == true
        if (!erLederpodEllerLokal) {
            logger.info("Ikke opprettet oppdaterLøpendeFlaggTask på denne poden")
            return
        }

        taskService.save(AvsluttUtløpteFagsakerTask.lagTask())
        logger.info("Opprettet oppdaterLøpendeFlaggTask")
    }

    @Scheduled(cron = "\${CRON_LAAS_FAGSAK_SCHEDULER}")
    fun startFagsakLåsingScheduled() {
        if (envService.erLokal() || LeaderClient.isLeader() == true) {
            startFagsakLåsing(maksAntall = STANDARD_MAKS_ANTALL_FAGSAKER_PER_KJØRING)
        }
    }

    fun startFagsakLåsing(maksAntall: Int): Boolean {
        if (!featureToggleService.isEnabled(FeatureToggle.FAGSAKLÅSING_SCHEDULER)) {
            logger.info("Fagsaklåsing-scheduler-toggle er av, hopper over batch")
            return false
        }

        taskService.save(FinnFagsakerSomSkalLåsesTask.opprettTask(maksAntall = maksAntall))
        logger.info("Opprettet FinnFagsakerSomSkalLåsesTask med maks $maksAntall fagsaker")
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)

        // Maks antall fagsaker som låses per automatiske kjøring. Holdes lavt i oppstarten og økes etter hvert.
        const val STANDARD_MAKS_ANTALL_FAGSAKER_PER_KJØRING = 100
    }
}
