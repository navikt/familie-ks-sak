package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.common.util.erHelligdag
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.GrensesnittavstemmingTaskDto
import no.nav.familie.ks.sak.task.utledNesteTriggerTidIHverdagerForTask
import no.nav.familie.leader.LeaderClient
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@Service
class GrensesnittavstemmingScheduler(
    private val taskService: TaskRepositoryWrapper,
    private val envService: EnvService,
) {
    @Scheduled(cron = "\${CRON_GRENSESNITT_AVSTEMMING}")
    fun utfør() {
        MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
        logger.info("Starter GrensesnittavstemmingScheduler..")

        if ((LeaderClient.isLeader() != true && !envService.erLokal()) || LocalDate.now().erHelligdag()) {
            // envService.erLokal() sjekk er lagt slik at scheduler-en kan testes i lokalt
            return
        }
        logger.info("Finner siste kjørte ${GrensesnittavstemmingTask.TASK_STEP_TYPE}")
        val alleFerdigeGrensesnittavstemmingTasker =
            taskService.finnTasksMedStatus(
                status = listOf(Status.FERDIG),
                type = GrensesnittavstemmingTask.TASK_STEP_TYPE,
                page = Pageable.unpaged(),
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
            tom =
                utledNesteTriggerTidIHverdagerForTask(
                    triggerTid = fom,
                    minimumForsinkelse = Duration.ofHours(24),
                ).toLocalDate().atStartOfDay()
        }
        // Opprett GrensesnittavstemmingTask
        taskService.save(GrensesnittavstemmingTask.opprettTask(fom, tom))

        logger.info("Stopper GrensesnittavstemmingScheduler..")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(GrensesnittavstemmingScheduler::class.java)
    }
}
