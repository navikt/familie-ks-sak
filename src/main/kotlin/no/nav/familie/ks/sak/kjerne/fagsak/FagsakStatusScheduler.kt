package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.task.FinnFagsakerSomSkalLåsesTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
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
            startFagsakLåsing()
        }
    }

    fun startFagsakLåsing(): Boolean {
        if (!featureToggleService.isEnabled(FeatureToggle.FAGSAKLÅSING_SCHEDULER)) {
            logger.info("Fagsaklåsing-scheduler-toggle er av, hopper over batch")
            return false
        }

        taskService.save(Task(type = FinnFagsakerSomSkalLåsesTask.TASK_STEP_TYPE, payload = ""))
        logger.info("Opprettet FinnFagsakerSomSkalLåsesTask")
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}
