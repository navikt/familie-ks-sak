package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.ForhåndsvisTilbakekrevingVarselbrevDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilbakekrevingControllerEnhetTest {
    private val tilgangService = mockk<TilgangService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()

    private val tilbakekrevingController =
        TilbakekrevingController(
            tilgangService = tilgangService,
            tilbakekrevingService = tilbakekrevingService,
            tilbakekrevingsbehandlingHentService = mockk(),
        )

    private val behandlingId = 1L
    private val forhåndsvisTilbakekrevingVarselbrevDto = ForhåndsvisTilbakekrevingVarselbrevDto(fritekst = "Fritekst")

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { tilbakekrevingService.hentForhåndsvisningTilbakekrevingVarselBrev(behandlingId, forhåndsvisTilbakekrevingVarselbrevDto) } returns ByteArray(0)
    }

    @Test
    fun `skal validere tilgang til fagsaken for behandlingen ved forhåndsvisning av varselbrev`() {
        // Act
        tilbakekrevingController.hentForhåndsvisningVarselbrev(behandlingId, forhåndsvisTilbakekrevingVarselbrevDto)

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandlingId,
                event = AuditLoggerEvent.ACCESS,
                minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                handling = "hente forhåndsvisning av varselbrev for tilbakekreving",
            )
        }
    }

    @Test
    fun `skal ikke generere varselbrev når saksbehandler mangler tilgang til fagsaken`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.ACCESS, BehandlerRolle.VEILEDER, any())
        } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsaken")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            tilbakekrevingController.hentForhåndsvisningVarselbrev(behandlingId, forhåndsvisTilbakekrevingVarselbrevDto)
        }

        verify(exactly = 0) { tilbakekrevingService.hentForhåndsvisningTilbakekrevingVarselBrev(any(), any()) }
    }
}
