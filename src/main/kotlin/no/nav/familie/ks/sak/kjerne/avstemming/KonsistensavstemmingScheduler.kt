package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingTaskDto
import no.nav.familie.leader.LeaderClient
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class KonsistensavstemmingScheduler(
    private val envService: EnvService,
    private val konsistensavstemmingKjøreplanService: KonsistensavstemmingKjøreplanService,
    private val taskService: TaskRepositoryWrapper,
) {
    @Scheduled(cron = "\${CRON_KONSISTENS_AVSTEMMING}")
    @Transactional
    fun utfør() {
        MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
        logger.info("Starter KonsistensavstemmingScheduler..")

        if (LeaderClient.isLeader() != true && !envService.erLokal()) {
            // envService.erLokal() sjekk er lagt slik at scheduler-en kan testes i lokalt
            return
        }
        val kjøreplan = konsistensavstemmingKjøreplanService.plukkLedigKjøreplanFor(LocalDate.now()) ?: return

        taskService.save(
            KonsistensavstemmingTask.opprettTask(
                KonsistensavstemmingTaskDto(kjøreplanId = kjøreplan.id, initieltKjøreTidspunkt = LocalDateTime.now()),
            ),
        )
        logger.info("Stopper KonsistensavstemmingScheduler..")
        MDC.clear() // rydder opp MDC
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(KonsistensavstemmingScheduler::class.java)
    }
}
