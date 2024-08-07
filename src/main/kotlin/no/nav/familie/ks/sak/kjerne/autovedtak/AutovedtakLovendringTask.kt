package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Trigger autovedtak av lovendring",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class AutovedtakLovendringTask(
    val autovedtakLovendringService: AutovedtakLovendringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        autovedtakLovendringService.revurderFagsak(fagsakId = task.payload.toLong())
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakLovendringTask::class.java)
        const val TASK_STEP_TYPE = "autovedtakLovendring"

        fun opprettTask(
            fagsakId: Long,
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = fagsakId.toString(),
                properties =
                    Properties().apply {
                        this["fagsakId"] = fagsakId.toString()
                    },
            )
        }
    }
}
