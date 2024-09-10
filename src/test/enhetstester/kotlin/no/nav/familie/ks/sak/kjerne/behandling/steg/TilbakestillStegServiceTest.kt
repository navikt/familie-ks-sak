package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TilbakestillStegServiceTest {
    private val mockedBehandlingRepository: BehandlingRepository = mockk()
    private val tilbakestillStegService: TilbakestillStegService = TilbakestillStegService(mockedBehandlingRepository)

    @Test
    fun `tilbakeførSteg skal tilbakeføre til behandling steg`() {
        // Arrange
        val behandling =
            lagBehandling(
                lagBehandlingStegTilstander = {
                    setOf(
                        lagBehandlingStegTilstand(
                            behandling = it,
                            behandlingSteg = BehandlingSteg.VILKÅRSVURDERING,
                            behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                        ),
                        lagBehandlingStegTilstand(
                            behandling = it,
                            behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT,
                            behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                        ),
                        lagBehandlingStegTilstand(
                            behandling = it,
                            behandlingSteg = BehandlingSteg.SIMULERING,
                            behandlingStegStatus = BehandlingStegStatus.KLAR,
                        ),
                    )
                },
            )

        every { mockedBehandlingRepository.hentAktivBehandling(behandling.id) }.returns(behandling)
        every { mockedBehandlingRepository.saveAndFlush(behandling) }.returns(behandling)

        // Act
        tilbakestillStegService.tilbakeførSteg(behandling.id, BehandlingSteg.VILKÅRSVURDERING)

        // Assert
        assertBehandlingHarStegMedStatus(behandling, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.KLAR)
        assertBehandlingHarStegMedStatus(behandling, BehandlingSteg.BEHANDLINGSRESULTAT, BehandlingStegStatus.TILBAKEFØRT)
        assertBehandlingHarStegMedStatus(behandling, BehandlingSteg.SIMULERING, BehandlingStegStatus.TILBAKEFØRT)
        verify(exactly = 1) { mockedBehandlingRepository.saveAndFlush(behandling) }
    }

    @Test
    fun `tilbakeførSteg skal ikke tilbakeføre til behandling steg når behandling er på samme steg`() {
        // Arrange
        val behandling =
            lagBehandling(
                lagBehandlingStegTilstander = {
                    setOf(
                        lagBehandlingStegTilstand(
                            behandling = it,
                            behandlingSteg = BehandlingSteg.VILKÅRSVURDERING,
                            behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                        ),
                        lagBehandlingStegTilstand(
                            behandling = it,
                            behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT,
                            behandlingStegStatus = BehandlingStegStatus.UTFØRT,
                        ),
                        lagBehandlingStegTilstand(
                            behandling = it,
                            behandlingSteg = BehandlingSteg.SIMULERING,
                            behandlingStegStatus = BehandlingStegStatus.KLAR,
                        ),
                    )
                },
            )

        every { mockedBehandlingRepository.hentAktivBehandling(behandling.id) }.returns(behandling)
        every { mockedBehandlingRepository.saveAndFlush(behandling) }.returns(behandling)

        // Act
        tilbakestillStegService.tilbakeførSteg(behandling.id, BehandlingSteg.SIMULERING)

        // Assert
        assertBehandlingHarStegMedStatus(behandling, BehandlingSteg.VILKÅRSVURDERING, BehandlingStegStatus.UTFØRT)
        assertBehandlingHarStegMedStatus(behandling, BehandlingSteg.BEHANDLINGSRESULTAT, BehandlingStegStatus.UTFØRT)
        assertBehandlingHarStegMedStatus(behandling, BehandlingSteg.SIMULERING, BehandlingStegStatus.KLAR)
        verify(exactly = 0) { mockedBehandlingRepository.saveAndFlush(any()) }
    }

    private fun assertBehandlingHarStegMedStatus(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg,
        behandlingStegStatus: BehandlingStegStatus,
    ) = assertTrue(
        behandling.behandlingStegTilstand.any {
            it.behandlingSteg == behandlingSteg &&
                it.behandlingStegStatus == behandlingStegStatus
        },
    )
}
