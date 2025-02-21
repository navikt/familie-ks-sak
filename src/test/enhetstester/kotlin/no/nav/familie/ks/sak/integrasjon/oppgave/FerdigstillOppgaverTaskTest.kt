package no.nav.familie.ks.sak.integrasjon.oppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test

class FerdigstillOppgaverTaskTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val behandlingService = mockk<BehandlingService>()

    private val ferdigstillOppgaverTask = FerdigstillOppgaverTask(oppgaveService, behandlingService)

    @Test
    fun `doTask skal forsøke å ferdigstille oppgave som ble lagret ned`() {
        val mockedTask = mockk<Task>()
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        every { mockedTask.payload } returns "{\"behandlingId\": 1,\"oppgavetype\":\"BehandleSak\"}"
        every { behandlingService.hentBehandling(1) } returns behandling
        every { oppgaveService.ferdigstillOppgaver(behandling, Oppgavetype.BehandleSak) } just runs

        ferdigstillOppgaverTask.doTask(mockedTask)

        verify(exactly = 1) { mockedTask.payload }
        verify(exactly = 1) { behandlingService.hentBehandling(1) }
        verify(exactly = 1) { oppgaveService.ferdigstillOppgaver(behandling, Oppgavetype.BehandleSak) }
    }
}
