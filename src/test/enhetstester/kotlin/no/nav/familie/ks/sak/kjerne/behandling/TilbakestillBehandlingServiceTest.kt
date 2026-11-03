package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.TilbakestillStegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import org.junit.jupiter.api.Test

class TilbakestillBehandlingServiceTest {
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()
    private val tilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val tilbakestillStegService = mockk<TilbakestillStegService>()

    private val tilbakestillBehandlingService =
        TilbakestillBehandlingService(
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            tilbakekrevingRepository = tilbakekrevingRepository,
            behandlingRepository = behandlingRepository,
            tilbakestillStegService = tilbakestillStegService,
        )

    @Test
    fun `skal tilbakeføre behandlingen til behandlingsresultat når behandlingen har passert behandlingsresultatet`() {
        // Arrange
        val behandling =
            lagBehandling(
                lagBehandlingStegTilstander = {
                    setOf(
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.VILKÅRSVURDERING, behandlingStegStatus = BehandlingStegStatus.UTFØRT),
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT, behandlingStegStatus = BehandlingStegStatus.UTFØRT),
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.SIMULERING, behandlingStegStatus = BehandlingStegStatus.KLAR),
                    )
                },
            )

        every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandling.id) } returns Unit
        every { tilbakekrevingRepository.findByBehandlingId(behandling.id) } returns null
        every { behandlingRepository.hentAktivBehandling(behandling.id) } returns behandling
        justRun { tilbakestillStegService.tilbakeførSteg(behandling.id, BehandlingSteg.BEHANDLINGSRESULTAT) }

        // Act
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)

        // Assert
        verify(exactly = 1) { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandling.id) }
        verify(exactly = 1) { tilbakestillStegService.tilbakeførSteg(behandling.id, BehandlingSteg.BEHANDLINGSRESULTAT) }
    }

    @Test
    fun `skal ikke flytte behandlingen fram til behandlingsresultat når behandlingen står på vilkårsvurdering`() {
        // Arrange
        val behandling =
            lagBehandling(
                lagBehandlingStegTilstander = {
                    setOf(
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.VILKÅRSVURDERING, behandlingStegStatus = BehandlingStegStatus.KLAR),
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT, behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT),
                    )
                },
            )

        every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandling.id) } returns Unit
        every { tilbakekrevingRepository.findByBehandlingId(behandling.id) } returns null
        every { behandlingRepository.hentAktivBehandling(behandling.id) } returns behandling

        // Act
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)

        // Assert
        verify(exactly = 1) { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandling.id) }
        verify(exactly = 0) { tilbakestillStegService.tilbakeførSteg(any(), any()) }
    }

    @Test
    fun `skal ikke flytte behandlingen fram til behandlingsresultat når behandlingen står på vent på vilkårsvurdering`() {
        // Arrange
        val behandling =
            lagBehandling(
                lagBehandlingStegTilstander = {
                    setOf(
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.VILKÅRSVURDERING, behandlingStegStatus = BehandlingStegStatus.VENTER),
                        lagBehandlingStegTilstand(behandling = it, behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT, behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT),
                    )
                },
            )

        every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandling.id) } returns Unit
        every { tilbakekrevingRepository.findByBehandlingId(behandling.id) } returns null
        every { behandlingRepository.hentAktivBehandling(behandling.id) } returns behandling

        // Act
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandling.id)

        // Assert
        verify(exactly = 0) { tilbakestillStegService.tilbakeførSteg(any(), any()) }
    }
}
