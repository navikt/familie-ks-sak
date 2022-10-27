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
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Assertions.assertEquals
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

        stegService.settBehandlingstegPåVent(
            behandling,
            frist
        )

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.VENTER, behandlingStegTilstand.behandlingStegStatus)
        assertEquals(frist, behandlingStegTilstand.frist)
        assertEquals(VenteÅrsak.AVVENTER_DOKUMENTASJON, behandlingStegTilstand.årsak)
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal oppdatere årsak og eller frist på behandlingstegtilstand til nåværende behandlingssteg og returnere gamle verdier for frist og årsak`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()

        val frist = LocalDate.now().plusWeeks(1)

        stegService.settBehandlingstegPåVent(
            behandling,
            frist
        )

        val nyFrist = LocalDate.now().plusMonths(1)

        val gammelFrist = stegService.oppdaterBehandlingstegFrist(
            behandling,
            nyFrist
        )

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.VENTER, behandlingStegTilstand.behandlingStegStatus)
        assertEquals(nyFrist, behandlingStegTilstand.frist)
        assertEquals(VenteÅrsak.AVVENTER_DOKUMENTASJON, behandlingStegTilstand.årsak)

        assertEquals(frist, gammelFrist)
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal ikke oppdatere årsak og eller frist på behandlingstegtilstand når frist og årsak er uendret`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()

        val frist = LocalDate.now().plusWeeks(1)

        stegService.settBehandlingstegPåVent(
            behandling,
            frist
        )

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.oppdaterBehandlingstegFrist(
                behandling,
                frist
            )
        }

        assertEquals(
            "Behandlingen er allerede satt på vent med frist $frist",
            funksjonellFeil.message
        )

        verify(exactly = 1) { behandlingRepository.saveAndFlush(any()) }
    }
}
