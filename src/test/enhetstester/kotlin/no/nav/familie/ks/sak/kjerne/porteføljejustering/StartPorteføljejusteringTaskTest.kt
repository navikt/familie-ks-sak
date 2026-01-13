package no.nav.familie.ks.sak.kjerne.porteføljejustering

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.VADSØ
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Test

class StartPorteføljejusteringTaskTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val taskService: TaskService = mockk()

    private val startPorteføljejusteringTask = StartPorteføljejusteringTask(integrasjonKlient, taskService)

    @Test
    fun `Skal hente kontantstøtte oppgaver hos enhet Vadsø og opprette task på flytting av enhet`() {
        // Arrange
        val finnOppgaveRequestForKonVadsø =
            FinnOppgaveRequest(
                tema = Tema.KON,
                enhet = VADSØ.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(id = 1, tildeltEnhetsnr = VADSØ.enhetsnummer, mappeId = 50, behandlingstype = Behandlingstype.NASJONAL.value),
                        Oppgave(id = 2, tildeltEnhetsnr = VADSØ.enhetsnummer, mappeId = 50, behandlingstype = Behandlingstype.NASJONAL.value),
                    ),
            )

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    task.type == PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE &&
                        task.payload == objectMapper.writeValueAsString(PorteføljejusteringFlyttOppgaveDto(1, VADSØ.enhetsnummer, 50))
                },
            )
        }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    task.type == PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE &&
                        task.payload == objectMapper.writeValueAsString(PorteføljejusteringFlyttOppgaveDto(2, VADSØ.enhetsnummer, 50))
                },
            )
        }
    }

    @Test
    fun `Skal ikke opprette opprette tasks hvis dryrun er satt til true`() {
        // Arrange
        val finnOppgaveRequestForKonVadsø =
            FinnOppgaveRequest(
                tema = Tema.KON,
                enhet = VADSØ.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(id = 1, tildeltEnhetsnr = VADSØ.enhetsnummer, mappeId = 50, behandlingstype = Behandlingstype.NASJONAL.value),
                        Oppgave(id = 2, tildeltEnhetsnr = VADSØ.enhetsnummer, mappeId = 50, behandlingstype = Behandlingstype.NASJONAL.value),
                    ),
            )

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = true)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) }
        verify(exactly = 0) {
            taskService.save(match { task -> task.type == PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE && task.payload == "1" })
        }
        verify(exactly = 0) {
            taskService.save(match { task -> task.type == PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE && task.payload == "2" })
        }
    }

    @Test
    fun `Skal ikke opprette flytte task på oppgaver som har infotrygd sak i saksreferanse`() {
        // Arrange
        val finnOppgaveRequestForKonVadsø =
            FinnOppgaveRequest(
                tema = Tema.KON,
                enhet = VADSØ.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 2,
                oppgaver =
                    listOf(
                        Oppgave(
                            id = 1,
                            tildeltEnhetsnr = VADSØ.enhetsnummer,
                            mappeId = 50,
                            saksreferanse = "12B34",
                            behandlingstype = Behandlingstype.NASJONAL.value,
                        ),
                        Oppgave(id = 2, tildeltEnhetsnr = VADSØ.enhetsnummer, mappeId = 50, saksreferanse = "IT01", behandlingstype = Behandlingstype.NASJONAL.value),
                    ),
            )

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) }
        verify(exactly = 0) {
            taskService.save(
                match { task ->
                    task.type == PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE &&
                        task.payload == objectMapper.writeValueAsString(PorteføljejusteringFlyttOppgaveDto(1, VADSØ.enhetsnummer, 50))
                },
            )
        }
        verify(exactly = 1) {
            taskService.save(
                match { task ->
                    task.type == PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE &&
                        task.payload == objectMapper.writeValueAsString(PorteføljejusteringFlyttOppgaveDto(2, VADSØ.enhetsnummer, 50))
                },
            )
        }
    }

    @Test
    fun `Skal ikke opprette flytte task på oppgaver som ikke har mappeid`() {
        // Arrange
        val finnOppgaveRequestForKonVadsø =
            FinnOppgaveRequest(
                tema = Tema.KON,
                enhet = VADSØ.enhetsnummer,
            )
        every { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = 1,
                oppgaver =
                    listOf(
                        Oppgave(
                            id = 1,
                            tildeltEnhetsnr = VADSØ.enhetsnummer,
                            mappeId = null,
                            saksreferanse = "12345",
                        ),
                    ),
            )

        every { taskService.save(any()) } returns mockk()

        val task = StartPorteføljejusteringTask.opprettTask(dryRun = false)

        // Act
        startPorteføljejusteringTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.hentOppgaver(finnOppgaveRequestForKonVadsø) }
        verify { taskService wasNot Called }
    }
}
