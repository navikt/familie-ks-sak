package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.avstemming.domene.GrensesnittavstemmingTaskDto
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = GrensesnittavstemmingTask.TASK_STEP_TYPE,
    beskrivelse = "Grensesnittavstemming mot oppdrag",
    maxAntallFeil = 2,
    triggerTidVedFeilISekunder = 60 * 5,
)
class GrensesnittavstemmingTask(
    private val avstemmingService: AvstemmingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue(task.payload, GrensesnittavstemmingTaskDto::class.java)
        logger.info("Kjører $TASK_STEP_TYPE for fom=${taskData.fom}, tom=${taskData.tom}, avstemmingId=${taskData.avstemmingId}")
        try {
            avstemmingService.sendGrensesnittavstemming(fom = taskData.fom, tom = taskData.tom, avstemmingId = taskData.avstemmingId)
        } catch (e: Exception) {
            throw Feil("Sjekk callid i kibana for å sjekke om avstemming ble kjørt ok i familie-oppdrag. Hvis avstemming kjørte ok, så kan tasken rekjøres uten at ny trigges.", throwable = e)
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "grensesnittavstemming"

        fun opprettTask(
            fom: LocalDateTime,
            tom: LocalDateTime,
            avstemmingId: UUID = UUID.randomUUID(),
        ) = Task(
            type = TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(GrensesnittavstemmingTaskDto(fom, tom, avstemmingId)),
            properties =
                Properties().apply {
                    // la til denne i properties slik at de kan vises i familie-prosessering
                    this["fom"] = fom.toString()
                    this["tom"] = tom.toString()
                    this["avstemmingId"] = avstemmingId.toString()
                },
        )

        private val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingTask::class.java)
    }
}
