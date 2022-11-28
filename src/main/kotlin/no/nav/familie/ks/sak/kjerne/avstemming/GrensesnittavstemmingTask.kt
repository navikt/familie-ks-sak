package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.GrensesnittavstemmingTaskDto
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = GrensesnittavstemmingTask.TASK_STEP_TYPE,
    beskrivelse = "Grensesnittavstemming mot oppdrag",
    maxAntallFeil = 3
)
class GrensesnittavstemmingTask(private val avstemmingService: AvstemmingService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue(task.payload, GrensesnittavstemmingTaskDto::class.java)
        logger.info("Kjører $TASK_STEP_TYPE for fom=${taskData.fom}, tom=${taskData.tom}")
        avstemmingService.sendGrensesnittavstemming(taskData.fom, taskData.tom)
    }

    companion object {

        const val TASK_STEP_TYPE = "grensesnittavstemming"

        private val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingTask::class.java)
    }
}
