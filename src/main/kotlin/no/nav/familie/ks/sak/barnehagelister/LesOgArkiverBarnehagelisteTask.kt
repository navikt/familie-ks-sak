package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LesOgArkiverBarnehagelisteTask.TASK_STEP_TYPE,
    beskrivelse = "Les og arkiver barnehageliste",
    maxAntallFeil = 1,
)
class LesOgArkiverBarnehagelisteTask(
    val barnehageListeService: BarnehageListeService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        barnehageListeService.lesOgArkiverBarnehageliste(UUID.fromString(task.payload))
        logger.info("Leser og arkiverer barnehageliste med id ${task.payload}")
    }

    companion object {
        val logger = LoggerFactory.getLogger(LesOgArkiverBarnehagelisteTask::class.java)
        const val TASK_STEP_TYPE = "lesogarkiverbarnehageliste"

        fun opprettTask(
            barnehagelisteId: UUID,
            arkivreferanse: String,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = barnehagelisteId.toString(),
                properties =
                    Properties().apply {
                        this["AR-referanse"] = arkivreferanse
                    },
            )
    }
}
