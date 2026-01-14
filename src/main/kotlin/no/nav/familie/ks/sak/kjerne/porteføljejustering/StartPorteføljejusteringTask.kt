package no.nav.familie.ks.sak.kjerne.porteføljejustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = StartPorteføljejusteringTask.TASK_STEP_TYPE,
    beskrivelse = "Finne oppgaver som skal flyttes og opprette tasks for flytting",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class StartPorteføljejusteringTask(
    private val integrasjonKlient: IntegrasjonKlient,
    private val taskService: TaskService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val startPorteføljejusteringTaskDto: StartPorteføljejusteringTaskDto = objectMapper.readValue(task.payload)
        val oppgaverIVadsø =
            integrasjonKlient
                .hentOppgaver(
                    finnOppgaveRequest =
                        FinnOppgaveRequest(
                            tema = Tema.KON,
                            enhet = KontantstøtteEnhet.VADSØ.enhetsnummer,
                        ),
                ).oppgaver

        logger.info("Fant ${oppgaverIVadsø.size} kontantstøtte oppgaver i Vadsø")

        val oppgaverSomSkalFlyttes =
            oppgaverIVadsø
                .filterNot { it.saksreferanse?.matches("\\d+[A-Z]\\d+".toRegex()) == true } // Filtrere bort infotrygd-oppgaver
                .filterNot { it.mappeId == null } // Vi skal ikke flytte oppgaver som ikke har mappe id
                .filter { it.behandlingstype == Behandlingstype.NASJONAL.value }

        logger.info("Fant ${oppgaverSomSkalFlyttes.size} kontantstøtte oppgaver som skal flyttes")

        val totalAntallOppgaverSomSkalFlyttes = oppgaverSomSkalFlyttes.size
        var opprettedeTasks = 0

        if (!startPorteføljejusteringTaskDto.dryRun) {
            oppgaverSomSkalFlyttes
                .take(startPorteføljejusteringTaskDto.antallTasks ?: oppgaverSomSkalFlyttes.size)
                .forEach { oppgave ->
                    oppgave.id?.let {
                        taskService.save(
                            PorteføljejusteringFlyttOppgaveTask.opprettTask(
                                oppgaveId = it,
                                enhetId = oppgave.tildeltEnhetsnr!!,
                                mappeId = oppgave.mappeId,
                            ),
                        )
                        opprettedeTasks++
                    }
                }
        }

        logger.info("Antall oppgaver totalt:$totalAntallOppgaverSomSkalFlyttes, Antall tasks opprettet for flytting:$opprettedeTasks")
    }

    companion object {
        const val TASK_STEP_TYPE = "startPorteføljejusteringTask"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(
            antallTasks: Int? = null,
            behandlesAvApplikasjon: RelevanteApplikasjonerForPorteføljejustering? = null,
            dryRun: Boolean = true,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(StartPorteføljejusteringTaskDto(antallTasks, behandlesAvApplikasjon?.applikasjonsnavn, dryRun)),
            )
    }
}

enum class RelevanteApplikasjonerForPorteføljejustering(
    val applikasjonsnavn: String,
) {
    FAMILIE_KS_SAK("familie-ks-sak"),
    FAMILIE_KLAGE("familie-klage"),
    FAMILIE_TILBAKE("familie-tilbake"),
}
