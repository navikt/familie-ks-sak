package no.nav.familie.ks.sak.kjerne.behandling.steg.henleggbehandling

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.api.dto.HenleggÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.henleggbehandling.HenleggBehandlingTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Henlegg behandling", maxAntallFeil = 3)
class HenleggBehandlingTask(
    private val henleggBehandlingService: HenleggBehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val henleggBehandlingDto: HenleggBehandlingTaskDto =
            jsonMapper.readValue<HenleggBehandlingTaskDto>(task.payload)

        logger.info("Henlegger behandling med id ${henleggBehandlingDto.behandlingId} med årsak ${henleggBehandlingDto.henleggÅrsak} og begrunnelse ${henleggBehandlingDto.begrunnelse}")

        henleggBehandlingService.henleggBehandling(
            behandlingId = henleggBehandlingDto.behandlingId,
            henleggÅrsak = henleggBehandlingDto.henleggÅrsak,
            begrunnelse = henleggBehandlingDto.begrunnelse,
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "henleggBehandlingTask"
        private val logger: Logger = LoggerFactory.getLogger(HenleggBehandlingTask::class.java)

        fun opprettTask(
            behandlingId: Long,
            henleggÅrsak: HenleggÅrsak,
            begrunnelse: String,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        HenleggBehandlingTaskDto(
                            behandlingId = behandlingId,
                            henleggÅrsak = henleggÅrsak,
                            begrunnelse = begrunnelse,
                        ),
                    ),
            )
    }
}

data class HenleggBehandlingTaskDto(
    val behandlingId: Long,
    val henleggÅrsak: HenleggÅrsak,
    val begrunnelse: String,
)
