package no.nav.familie.ks.sak.task

import no.nav.familie.ks.sak.kjerne.fagsaklåsing.FagsakLåsingService
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = LåsFagsakTask.TASK_STEP_TYPE,
    beskrivelse = "Lås fagsak og send melding til statistikk og Joark",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class LåsFagsakTask(
    private val fagsakLåsingService: FagsakLåsingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()
        fagsakLåsingService.låsFagsak(fagsakId)
    }

    companion object {
        const val TASK_STEP_TYPE = "låsFagsakTask"

        fun opprettTask(fagsakId: Long): Task =
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                Task(
                    type = TASK_STEP_TYPE,
                    payload = fagsakId.toString(),
                )
            }
    }
}
