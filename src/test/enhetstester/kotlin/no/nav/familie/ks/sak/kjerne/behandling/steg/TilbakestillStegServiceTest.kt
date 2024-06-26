package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.BEHANDLINGSRESULTAT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.SIMULERING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg.VILKÅRSVURDERING
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.TILBAKEFØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.UTFØRT
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TilbakestillStegServiceTest {
    private val mockedBehandlingRepository: BehandlingRepository = mockk()
    private val tilbakestillStegService: TilbakestillStegService = TilbakestillStegService(mockedBehandlingRepository)

    @Test
    fun `tilbakeførSteg skal tilbakeføre til behandling steg`() {
        // Arrange
        val behandling = lagBehandling()
        behandling.behandlingStegTilstand.clear()
        lagBehandlingStegTilstand(behandling, VILKÅRSVURDERING, UTFØRT)
        lagBehandlingStegTilstand(behandling, BEHANDLINGSRESULTAT, UTFØRT)
        lagBehandlingStegTilstand(behandling, SIMULERING, KLAR)

        every { mockedBehandlingRepository.hentAktivBehandling(behandling.id) }.returns(behandling)
        every { mockedBehandlingRepository.saveAndFlush(behandling) }.returns(behandling)

        // Act
        tilbakestillStegService.tilbakeførSteg(behandling.id, VILKÅRSVURDERING)

        // Assert
        assertBehandlingHarStegMedStatus(behandling, VILKÅRSVURDERING, KLAR)
        assertBehandlingHarStegMedStatus(behandling, BEHANDLINGSRESULTAT, TILBAKEFØRT)
        assertBehandlingHarStegMedStatus(behandling, SIMULERING, TILBAKEFØRT)
        verify(exactly = 1) { mockedBehandlingRepository.saveAndFlush(behandling) }
    }

    @Test
    fun `tilbakeførSteg skal ikke tilbakeføre til behandling steg når behandling er på samme steg`() {
        // Arrange
        val behandling = lagBehandling()
        behandling.behandlingStegTilstand.clear()
        lagBehandlingStegTilstand(behandling, VILKÅRSVURDERING, UTFØRT)
        lagBehandlingStegTilstand(behandling, BEHANDLINGSRESULTAT, UTFØRT)
        lagBehandlingStegTilstand(behandling, SIMULERING, KLAR)

        every { mockedBehandlingRepository.hentAktivBehandling(behandling.id) }.returns(behandling)
        every { mockedBehandlingRepository.saveAndFlush(behandling) }.returns(behandling)

        // Act
        tilbakestillStegService.tilbakeførSteg(behandling.id, SIMULERING)

        // Assert
        assertBehandlingHarStegMedStatus(behandling, VILKÅRSVURDERING, UTFØRT)
        assertBehandlingHarStegMedStatus(behandling, BEHANDLINGSRESULTAT, UTFØRT)
        assertBehandlingHarStegMedStatus(behandling, SIMULERING, KLAR)
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
