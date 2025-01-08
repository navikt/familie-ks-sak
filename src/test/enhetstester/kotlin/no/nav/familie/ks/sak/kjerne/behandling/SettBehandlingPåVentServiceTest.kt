package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    fun `settBehandlingPåVent - skal sette behandling på vent og forlenge frist på oppgave`() {
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { stegService.settBehandlingstegPåVent(any(), any(), VenteÅrsak.AVVENTER_DOKUMENTASJON) } just runs
        every { oppgaveService.settNyFristÅpneOppgaverPåBehandling(any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        settBehandlingPåVentService.settBehandlingPåVent(
            behandling.id,
            frist,
            VenteÅrsak.AVVENTER_DOKUMENTASJON,
        )

        verify(exactly = 1) { behandlingRepository.hentBehandling(any()) }
        verify(exactly = 1) { stegService.settBehandlingstegPåVent(any(), any(), VenteÅrsak.AVVENTER_DOKUMENTASJON) }
        verify(exactly = 1) { oppgaveService.settNyFristÅpneOppgaverPåBehandling(any(), any()) }
    }

    @Test
    fun `settBehandlingPåVent - skal kaste feil når behandling allerede er satt på vent`() {
        behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }.behandlingStegStatus =
            BehandlingStegStatus.VENTER
        every { behandlingRepository.hentBehandling(any()) } returns behandling

        val frist = LocalDate.now().plusWeeks(1)

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                settBehandlingPåVentService.settBehandlingPåVent(
                    behandling.id,
                    frist,
                    VenteÅrsak.AVVENTER_DOKUMENTASJON,
                )
            }

        assertEquals("Behandlingen er allerede satt på vent.", funksjonellFeil.message)
    }

    @Test
    fun `settBehandlingPåVent - skal kaste feil når frist er før dagens dato`() {
        every { behandlingRepository.hentBehandling(any()) } returns behandling

        val frist = LocalDate.now().minusWeeks(1)

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                settBehandlingPåVentService.settBehandlingPåVent(
                    behandling.id,
                    frist,
                    VenteÅrsak.AVVENTER_DOKUMENTASJON,
                )
            }

        assertEquals(
            "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
            funksjonellFeil.message,
        )
    }

    @Test
    fun `settBehandlingPåVent - skal kaste feil når behandlingen er avsluttet`() {
        behandling.status = BehandlingStatus.AVSLUTTET
        every { behandlingRepository.hentBehandling(any()) } returns behandling

        val frist = LocalDate.now().plusWeeks(1)

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                settBehandlingPåVentService.settBehandlingPåVent(
                    behandling.id,
                    frist,
                    VenteÅrsak.AVVENTER_DOKUMENTASJON,
                )
            }

        assertEquals(
            "Behandling ${behandling.id} er avsluttet og kan ikke settes på vent.",
            funksjonellFeil.message,
        )
    }

    @Test
    fun `settBehandlingPåVent - skal kaste feil når behandlingen ikke er aktiv`() {
        behandling.aktiv = false
        every { behandlingRepository.hentBehandling(any()) } returns behandling

        val frist = LocalDate.now().plusWeeks(1)

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                settBehandlingPåVentService.settBehandlingPåVent(
                    behandling.id,
                    frist,
                    VenteÅrsak.AVVENTER_DOKUMENTASJON,
                )
            }

        assertEquals(
            "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent.",
            funksjonellFeil.message,
        )
    }

    @Test
    fun `oppdaterBehandlingPåVent - skal oppdatere frist på ventende behandling, og forlenge frist på oppgave`() {
        val gammelFrist = LocalDate.now().plusWeeks(1)
        val frist = LocalDate.now().plusWeeks(2)

        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every {
            stegService.oppdaterBehandlingstegFristOgÅrsak(
                any(),
                any(),
                VenteÅrsak.AVVENTER_DOKUMENTASJON,
            )
        } returns gammelFrist

        every { oppgaveService.settNyFristÅpneOppgaverPåBehandling(any(), any()) } just runs

        settBehandlingPåVentService.oppdaterFristOgÅrsak(
            behandling.id,
            frist,
            VenteÅrsak.AVVENTER_DOKUMENTASJON,
        )

        verify(exactly = 1) { behandlingRepository.hentBehandling(any()) }
        verify(exactly = 1) {
            stegService.oppdaterBehandlingstegFristOgÅrsak(
                any(),
                any(),
                VenteÅrsak.AVVENTER_DOKUMENTASJON,
            )
        }
        verify(exactly = 1) { oppgaveService.settNyFristÅpneOppgaverPåBehandling(any(), any()) }
    }

    @Test
    fun `gjenopptaBehandlingPåVent - skal gjenoppta ventende behandling, oppdatere logg og sette ny frist på oppgave`() {
        behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }.behandlingStegStatus =
            BehandlingStegStatus.VENTER
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { stegService.utførSteg(any(), any()) } just runs
        every { loggService.opprettBehandlingGjenopptattLogg(behandling) } just runs
        every { oppgaveService.settFristÅpneOppgaverPåBehandlingTil(any(), any()) } just runs

        settBehandlingPåVentService.gjenopptaBehandlingPåVent(
            behandling.id,
        )

        verify(exactly = 1) { behandlingRepository.hentBehandling(any()) }
        verify(exactly = 1) { stegService.utførSteg(any(), any()) }
        verify(exactly = 1) { loggService.opprettBehandlingGjenopptattLogg(any()) }
        verify(exactly = 1) { oppgaveService.settFristÅpneOppgaverPåBehandlingTil(any(), any()) }
    }
}
