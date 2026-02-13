package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.FerdigstillOppgaveDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgaverTask.TASK_STEP_TYPE,
    beskrivelse = "Ferdigstill oppgaver i GOSYS for behandling",
    maxAntallFeil = 3,
)
class FerdigstillOppgaverTask(
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val ferdigstillOppgave = jsonMapper.readValue(task.payload, FerdigstillOppgaveDto::class.java)
        val behandling = behandlingService.hentBehandling(ferdigstillOppgave.behandlingId)

        oppgaveService.ferdigstillOppgaver(
            behandling = behandling,
            oppgavetype = ferdigstillOppgave.oppgavetype,
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "ferdigstillOppgaveTask"

        fun opprettTask(
            behandlingId: Long,
            oppgavetype: Oppgavetype,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        FerdigstillOppgaveDto(
                            behandlingId = behandlingId,
                            oppgavetype = oppgavetype,
                        ),
                    ),
            )
    }
}
