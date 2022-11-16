package no.nav.familie.ks.sak.kjerne.behandling.steg.avsluttbehandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = AvsluttBehandlingTask.TASK_STEP_TYPE,
    beskrivelse = "Avslutt behandling",
    maxAntallFeil = 3
)
class AvsluttBehandlingTask(private val stegService: StegService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()
        stegService.utførSteg(behandlingId, BehandlingSteg.AVSLUTT_BEHANDLING)
    }

    companion object {

        const val TASK_STEP_TYPE = "avsluttBehandling"

        fun opprettTask(søkerIdent: String, behandlingId: Long): Task = Task(
            type = TASK_STEP_TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this["personIdent"] = søkerIdent
                this["behandlingsId"] = behandlingId.toString()
            }
        )
    }
}
