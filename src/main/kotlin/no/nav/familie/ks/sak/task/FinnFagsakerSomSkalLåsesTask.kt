package no.nav.familie.ks.sak.task

import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnFagsakerSomSkalLåsesTask.TASK_STEP_TYPE,
    beskrivelse = "Finn fagsaker som skal låses",
    maxAntallFeil = 3,
)
class FinnFagsakerSomSkalLåsesTask(
    private val taskService: TaskRepositoryWrapper,
    private val fagsakRepository: FagsakRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        fagsakRepository
            .finnAvsluttedeFagsakerSomSkalLåses()
            .also { logger.info("Fant ${it.size} fagsaker som skal låses") }
            .forEach { taskService.save(LåsFagsakTask.opprettTask(it)) }
    }

    companion object {
        const val TASK_STEP_TYPE = "finnFagsakerSomSkalLåsesTask"
        private val logger = LoggerFactory.getLogger(FinnFagsakerSomSkalLåsesTask::class.java)
    }
}
