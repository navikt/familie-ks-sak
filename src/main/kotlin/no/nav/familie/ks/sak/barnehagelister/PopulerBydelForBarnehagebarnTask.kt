package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = PopulerBydelForBarnehagebarnTask.TASK_STEP_TYPE,
    beskrivelse = "Populerer bydel for barnehagebarn i Oslo",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 60,
)
class PopulerBydelForBarnehagebarnTask(
    private val barnehageBydelService: BarnehageBydelService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        barnehageBydelService.oppdaterBydelOrgNummerPåBarnehagebarn(UUID.fromString(task.payload))
    }

    companion object {
        const val TASK_STEP_TYPE = "populerBydelForBarnehagebarnTask"

        fun opprettTask(barnehagebarnId: UUID): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = barnehagebarnId.toString(),
            )
    }
}
