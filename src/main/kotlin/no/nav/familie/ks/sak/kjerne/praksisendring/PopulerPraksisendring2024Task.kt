package no.nav.familie.ks.sak.kjerne.praksisendring

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.internal.PopulerPraksisendring2024TabellMedFagsakSomHarUtbetalingSammeMånedSomBarnehagestart
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = PopulerPraksisendring2024Task.TASK_STEP_TYPE,
    beskrivelse = "Populerer praksisendring 2024 tabellen",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class PopulerPraksisendring2024Task(
    private val populerPraksisendring2024TabellMedFagsakSomHarUtbetalingSammeMånedSomBarnehagestart: PopulerPraksisendring2024TabellMedFagsakSomHarUtbetalingSammeMånedSomBarnehagestart,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        secureLogger.info("Utfører PopulerPraksisendring2024Task.")

        populerPraksisendring2024TabellMedFagsakSomHarUtbetalingSammeMånedSomBarnehagestart.utfør()

        secureLogger.info("PopulerPraksisendring2024Task utført ok.")
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        const val TASK_STEP_TYPE = "PopulerPraksisendring2024Task"

        fun opprettTask(): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(LocalDateTime.now()),
            )
    }
}
