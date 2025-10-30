package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FagsakStatusScheduler(
    private val taskService: TaskRepositoryWrapper,
    private val envService: EnvService,
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

    companion object {
        private val logger = LoggerFactory.getLogger(FagsakStatusScheduler::class.java)
    }
}
