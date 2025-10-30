package no.nav.familie.ks.sak.integrasjon.økonomi.internkonsistensavstemming

import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class InternKonsistensavstemmingScheduler(
    val taskService: TaskRepositoryWrapper,
) {
    @Scheduled(cron = "0 0 0 16 * *")
    fun startInternKonsistensavstemming() {
        if (LeaderClient.isLeader() == true) {
            taskService.save(OpprettInternKonsistensavstemmingTaskerTask.opprettTask())
        }
    }
}
