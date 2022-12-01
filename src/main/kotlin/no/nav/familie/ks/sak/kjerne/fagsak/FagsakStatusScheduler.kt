import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(
    private val taskService: TaskService
) {

    /*
     * Siden kontantstøtte er en månedsytelse vil en fagsak alltid løpe ut en måned
     * Det er derfor nok å finne alle fagsaker som ikke lenger har noen løpende utbetalinger den 1 hver måned.
     */

    @Scheduled(cron = "\${CRON_FAGSAKSTATUS_SCHEDULER}")
    fun oppdaterFagsakStatuser() {
        if (LeaderClient.isLeader() == true) {
            val oppdaterLøpendeFlaggTask = Task(type = AvsluttUtløpteFagsakerTask.TASK_STEP_TYPE, payload = "")
            taskService.save(oppdaterLøpendeFlaggTask)
            logger.info("Opprettet oppdaterLøpendeFlaggTask")
        } else {
            logger.info("Ikke opprettet oppdaterLøpendeFlaggTask på denne poden")
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}
