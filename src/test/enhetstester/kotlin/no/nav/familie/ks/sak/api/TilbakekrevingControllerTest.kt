package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TilbakekrevingControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val tilbakekrevingService = mockk<TilbakekrevingService>()
    private val tilbakekrevingController =
        TilbakekrevingController(
            tilgangService = tilgangService,
            tilbakekrevingService = tilbakekrevingService,
            tilbakekrevingsbehandlingHentService = mockk(),
        )

    private val behandling = lagBehandling()
    private val tilbakekreving =
        Tilbakekreving(
            behandling = behandling,
            valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            begrunnelse = "Feilutbetalingen skal kreves tilbake",
            tilbakekrevingsbehandlingId = null,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
    }

    @Nested
    inner class HentTilbakekrevingPåBehandling {
        @Test
        fun `skal returnere tilbakekrevingen på behandlingen`() {
            // Arrange
            every { tilbakekrevingService.finnTilbakekrevingsbehandling(behandlingId = behandling.id) } returns tilbakekreving

            // Act
            val respons = tilbakekrevingController.hentTilbakekrevingPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.data?.valg).isEqualTo(tilbakekreving.valg)
            assertThat(respons.body?.data?.begrunnelse).isEqualTo(tilbakekreving.begrunnelse)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har tilbakekreving`() {
            // Arrange
            every { tilbakekrevingService.finnTilbakekrevingsbehandling(behandlingId = behandling.id) } returns null

            // Act
            val respons = tilbakekrevingController.hentTilbakekrevingPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }
    }
}
