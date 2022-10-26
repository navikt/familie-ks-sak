package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class StegServiceUnitTest {

    @MockK
    private lateinit var steg: List<IBehandlingSteg>

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @InjectMockKs
    private lateinit var stegService: StegService

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `settBehandlingstegTilstandPåVent - skal sette årsak frist og status på behandlingstegtilstand til nåværende behandlingssteg`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()

        val frist = LocalDate.now().plusWeeks(1)

        stegService.settBehandlingstegTilstandPåVent(
            behandling,
            frist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.VENTER, behandlingStegTilstand.behandlingStegStatus)
        assertEquals(frist, behandlingStegTilstand.frist)
        assertEquals(BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON, behandlingStegTilstand.årsak)
    }

    @Test
    fun `settBehandlingstegTilstandPåVent - skal kaste feil når behandling allerede er satt på vent`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()

        val frist = LocalDate.now().plusWeeks(1)

        stegService.settBehandlingstegTilstandPåVent(
            behandling,
            frist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.settBehandlingstegTilstandPåVent(
                behandling,
                frist,
                BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        }

        assertEquals("Behandlingen er allerede satt på vent.", funksjonellFeil.message)
    }

    @Test
    fun `settBehandlingstegTilstandPåVent - skal kaste feil når frist er før dagens dato`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()

        val frist = LocalDate.now().minusWeeks(1)

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.settBehandlingstegTilstandPåVent(
                behandling,
                frist,
                BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        }

        assertEquals(
            "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
            funksjonellFeil.message
        )
    }

    @Test
    fun `settBehandlingstegTilstandPåVent - skal kaste feil når behandlingen er avsluttet`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()

        behandling.status = BehandlingStatus.AVSLUTTET

        val frist = LocalDate.now().plusWeeks(1)

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.settBehandlingstegTilstandPåVent(
                behandling,
                frist,
                BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        }

        assertEquals(
            "Behandling ${behandling.id} er avsluttet og kan ikke settes på vent.",
            funksjonellFeil.message
        )
    }

    @Test
    fun `settBehandlingstegTilstandPåVent - skal kaste feil når behandlingen ikke er aktiv`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()

        behandling.aktiv = false

        val frist = LocalDate.now().plusWeeks(1)

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.settBehandlingstegTilstandPåVent(
                behandling,
                frist,
                BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        }

        assertEquals(
            "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent.",
            funksjonellFeil.message
        )
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal oppdatere årsak og eller frist på behandlingstegtilstand til nåværende behandlingssteg og returnere gamle verdier for frist og årsak`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()

        val frist = LocalDate.now().plusWeeks(1)

        stegService.settBehandlingstegTilstandPåVent(
            behandling,
            frist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        val nyFrist = LocalDate.now().plusMonths(1)

        val gamleVerdier = stegService.oppdaterBehandlingstegTilstandPåVent(
            behandling,
            nyFrist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.VENTER, behandlingStegTilstand.behandlingStegStatus)
        assertEquals(nyFrist, behandlingStegTilstand.frist)
        assertEquals(BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON, behandlingStegTilstand.årsak)

        assertEquals(frist, gamleVerdier.first)
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal ikke oppdatere årsak og eller frist på behandlingstegtilstand når frist og årsak er uendret`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()

        val frist = LocalDate.now().plusWeeks(1)

        stegService.settBehandlingstegTilstandPåVent(
            behandling,
            frist,
            BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
        )

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.oppdaterBehandlingstegTilstandPåVent(
                behandling,
                frist,
                BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON
            )
        }

        assertEquals(
            "Behandlingen er allerede satt på vent med frist $frist og årsak ${BehandlingSettPåVentÅrsak.AVVENTER_DOKUMENTASJON}.",
            funksjonellFeil.message
        )

        verify(exactly = 1) { behandlingRepository.saveAndFlush(any()) }
    }

    @Test
    fun `gjenopptaBehandlingstegTilstandPåVent - skal resette frist og årsak samt sette behandlingstegStatus til KLAR`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()

        stegService.gjenopptaBehandlingstegTilstandPåVent(behandling)

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.KLAR, behandlingStegTilstand.behandlingStegStatus)
        assertNull(behandlingStegTilstand.frist)
        assertNull(behandlingStegTilstand.årsak)
    }
}
