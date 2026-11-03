package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.EndreBehandlingstemaDto
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehandlingControllerUnitTest {
    private val behandlingService = mockk<BehandlingService>()
    private val tilgangService = mockk<TilgangService>()
    private val tilbakestillBehandlingService = mockk<TilbakestillBehandlingService>()

    private val behandlingController =
        BehandlingController(
            opprettBehandlingService = mockk(),
            behandlingService = behandlingService,
            tilgangService = tilgangService,
            settBehandlingPåVentService = mockk(),
            henleggBehandlingService = mockk(),
            tilkjentYtelseValideringService = mockk(),
            leggTilBarnService = mockk(),
            tilbakestillBehandlingService = tilbakestillBehandlingService,
        )

    private val behandling = lagBehandling(id = 1, kategori = BehandlingKategori.NASJONAL)

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { behandlingService.endreBehandlingstemaPåBehandling(behandling.id, any()) } answers {
            behandling.apply { kategori = secondArg() }
        }
        justRun { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling.id) }
        every { behandlingService.lagBehandlingRespons(behandling.id) } returns mockk()
    }

    @Test
    fun `skal tilbakestille behandlingen til vilkårsvurdering når kategori endres`() {
        // Act
        behandlingController.endreBehandlingstema(behandling.id, EndreBehandlingstemaDto(behandlingKategori = BehandlingKategori.EØS))

        // Assert
        verify(exactly = 1) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandling.id) }
    }

    @Test
    fun `skal ikke tilbakestille behandlingen når kategori er uendret`() {
        // Act
        behandlingController.endreBehandlingstema(behandling.id, EndreBehandlingstemaDto(behandlingKategori = BehandlingKategori.NASJONAL))

        // Assert
        verify(exactly = 0) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(any()) }
    }

    @Test
    fun `skal ikke tilbakestille behandlingen når den ikke har status utredes`() {
        // Arrange
        behandling.status = BehandlingStatus.FATTER_VEDTAK

        // Act
        behandlingController.endreBehandlingstema(behandling.id, EndreBehandlingstemaDto(behandlingKategori = BehandlingKategori.EØS))

        // Assert
        verify(exactly = 0) { tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(any()) }
    }
}
