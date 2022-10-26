package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSettPåVentÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SettBehandlingPåVentServiceTest {
    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var stegService: StegService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @InjectMockKs
    private lateinit var settBehandlingPåVentService: SettBehandlingPåVentService

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `settBehandlingPåVent - skal sette behandling på vent, oppdatere logg og forlenge frist på oppgave`() {
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { stegService.settBehandlingstegTilstandPåVent(any(), any(), any()) } just runs
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
        every { oppgaveService.forlengFristÅpneOppgaverPåBehandling(any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        settBehandlingPåVentService.settBehandlingPåVent(
            behandling.id,
            frist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        verify(exactly = 1) { behandlingRepository.hentBehandling(any()) }
        verify(exactly = 1) { stegService.settBehandlingstegTilstandPåVent(any(), any(), any()) }
        verify(exactly = 1) { loggService.opprettSettPåVentLogg(any(), any()) }
        verify(exactly = 1) { oppgaveService.forlengFristÅpneOppgaverPåBehandling(any(), any()) }
    }

    @Test
    fun `oppdaterBehandlingPåVent - skal oppdatere frist på ventende behandling, oppdatere logg og forlenge frist på oppgave`() {
        val gammelFrist = LocalDate.now().plusWeeks(1)
        val frist = LocalDate.now().plusWeeks(2)

        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { stegService.oppdaterBehandlingstegTilstandPåVent(any(), any(), any()) } returns Pair(
            gammelFrist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )
        every { loggService.opprettOppdaterVentingLogg(behandling, null, frist) } just runs
        every { oppgaveService.forlengFristÅpneOppgaverPåBehandling(any(), any()) } just runs

        settBehandlingPåVentService.oppdaterBehandlingPåVent(
            behandling.id,
            frist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        verify(exactly = 1) { behandlingRepository.hentBehandling(any()) }
        verify(exactly = 1) { stegService.oppdaterBehandlingstegTilstandPåVent(any(), any(), any()) }
        verify(exactly = 1) { loggService.opprettOppdaterVentingLogg(any(), any(), any()) }
        verify(exactly = 1) { oppgaveService.forlengFristÅpneOppgaverPåBehandling(any(), any()) }
    }

    @Test
    fun `gjenopptaBehandlingPåVent - skal gjenoppta ventende behandling, oppdatere logg og sette ny frist på oppgave`() {
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { stegService.gjenopptaBehandlingstegTilstandPåVent(any()) } just runs
        every { loggService.opprettBehandlingGjenopptattLogg(behandling) } just runs
        every { oppgaveService.settFristÅpneOppgaverPåBehandlingTil(any(), any()) } just runs

        settBehandlingPåVentService.gjenopptaBehandlingPåVent(
            behandling.id
        )

        verify(exactly = 1) { behandlingRepository.hentBehandling(any()) }
        verify(exactly = 1) { stegService.gjenopptaBehandlingstegTilstandPåVent(any()) }
        verify(exactly = 1) { loggService.opprettBehandlingGjenopptattLogg(any()) }
        verify(exactly = 1) { oppgaveService.settFristÅpneOppgaverPåBehandlingTil(any(), any()) }
    }
}
