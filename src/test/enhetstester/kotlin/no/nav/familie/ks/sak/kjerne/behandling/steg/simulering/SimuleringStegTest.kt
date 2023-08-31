package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.api.dto.TilbakekrevingRequestDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
internal class SimuleringStegTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var simuleringService: SimuleringService

    @MockK
    private lateinit var tilbakekrevingService: TilbakekrevingService

    @InjectMockKs
    private lateinit var simuleringSteg: SimuleringSteg

    private val revurderingsbehandling = lagBehandling()

    @BeforeEach
    fun setup() {
        every { behandlingService.hentBehandling(revurderingsbehandling.id) } returns revurderingsbehandling
        every { tilbakekrevingService.lagreTilbakekreving(any(), any()) } returns mockk()
    }

    @Test
    fun `skal utføre steg for revurdering når det ikke finnes åpen tilbakekrevingsbehandling`() {
        every { tilbakekrevingService.harÅpenTilbakekrevingsbehandling(revurderingsbehandling.fagsak.id) } returns false
        every { simuleringService.hentFeilutbetaling(revurderingsbehandling.id) } returns BigDecimal(3500)

        assertDoesNotThrow { simuleringSteg.utførSteg(revurderingsbehandling.id, lagTilbakekrevingDto()) }
        verify(exactly = 1) { simuleringService.hentFeilutbetaling(revurderingsbehandling.id) }
        verify(exactly = 1) { tilbakekrevingService.lagreTilbakekreving(any(), any()) }
    }

    @Test
    fun `skal utføre steg for revurdering når det finnes åpen tilbakekrevingsbehandling`() {
        every { tilbakekrevingService.harÅpenTilbakekrevingsbehandling(revurderingsbehandling.fagsak.id) } returns true

        assertDoesNotThrow { simuleringSteg.utførSteg(revurderingsbehandling.id, lagTilbakekrevingDto()) }
        verify(exactly = 0) { simuleringService.hentFeilutbetaling(revurderingsbehandling.id) }
        verify(exactly = 0) { tilbakekrevingService.lagreTilbakekreving(any(), any()) }
    }

    @Test
    fun `skal ikke utføre steg for revurdering når det ikke finnes en feilutbetaling men frontend sender tilbakekrevingDto`() {
        every { tilbakekrevingService.harÅpenTilbakekrevingsbehandling(revurderingsbehandling.fagsak.id) } returns false
        every { simuleringService.hentFeilutbetaling(revurderingsbehandling.id) } returns BigDecimal.ZERO

        val exception = assertThrows<FunksjonellFeil> {
            simuleringSteg.utførSteg(revurderingsbehandling.id, lagTilbakekrevingDto())
        }
        assertEquals("Simuleringen har ikke en feilutbetaling, men tilbakekrevingDto var ikke null", exception.message)
        assertEquals("Du kan ikke opprette en tilbakekreving når det ikke er en feilutbetaling.", exception.frontendFeilmelding)
    }

    private fun lagTilbakekrevingDto() = TilbakekrevingRequestDto(
        valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
        varsel = "Opprett en tilbakekreving",
        begrunnelse = "Test begrunnelse",
    )
}
