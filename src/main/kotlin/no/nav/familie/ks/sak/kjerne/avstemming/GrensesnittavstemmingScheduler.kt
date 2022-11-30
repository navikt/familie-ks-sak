package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.util.erHelligdag
import no.nav.familie.ks.sak.kjerne.avstemming.domene.GrensesnittavstemmingTaskDto
import no.nav.familie.ks.sak.task.nesteGyldigeTriggertidForBehandlingIHverdager
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class GrensesnittavstemmingScheduler(private val taskService: TaskService) {

    @Scheduled(cron = "\${CRON_GRENSESNITT_AVSTEMMING}")
    fun utfør() {
        logger.info("Starter GrensesnittavstemmingScheduler med leader client ${LeaderClient.isLeader()}..")
        if (LeaderClient.isLeader() != true || LocalDate.now().erHelligdag()) {
            return
        }
        logger.info("Finner siste kjørte ${GrensesnittavstemmingTask.TASK_STEP_TYPE}")
        val alleFerdigeGrensesnittavstemmingTasker = taskService.finnTasksMedStatus(
            status = listOf(Status.FERDIG),
            type = GrensesnittavstemmingTask.TASK_STEP_TYPE,
            page = Pageable.unpaged()
        )
        // intielt fom, tom når det ikke finnes noen tasker
        var fom = LocalDate.now().minusDays(1).atStartOfDay()
        var tom = LocalDate.now().atStartOfDay()

        // setter fom og tom basert på task data fra siste kjørte task
        if (alleFerdigeGrensesnittavstemmingTasker.isNotEmpty()) {
            val sisteFerdigTask = alleFerdigeGrensesnittavstemmingTasker.maxBy { it.opprettetTid }
            logger.info("Fant siste ferdig task med id=${sisteFerdigTask.id}")
            val sisteFerdigTaskData =
                objectMapper.readValue(sisteFerdigTask.payload, GrensesnittavstemmingTaskDto::class.java)
            fom = sisteFerdigTaskData.tom.toLocalDate().atStartOfDay()
            tom = nesteGyldigeTriggertidForBehandlingIHverdager((24 * 60).toLong(), fom).toLocalDate().atStartOfDay()
        }
        val grensesnittavstemmingTaskDto = GrensesnittavstemmingTaskDto(fom, tom)

        taskService.save(
            Task(
                type = GrensesnittavstemmingTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(grensesnittavstemmingTaskDto)
            )
        )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingTask::class.java)
    }
}
