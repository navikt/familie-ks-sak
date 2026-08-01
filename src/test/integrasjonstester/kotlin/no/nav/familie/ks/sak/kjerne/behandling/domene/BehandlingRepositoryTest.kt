package no.nav.familie.ks.sak.kjerne.behandling.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `hentBehandling - skal finne behandling med behandlingsId`() {
        // Act
        val hentetBehandling = behandlingRepository.hentBehandling(behandling.id)

        // Assert
        assertEquals(behandling.id, hentetBehandling.id)
    }

    @Test
    fun `hentAktivBehandling - skal finne behandling med behandlingsId som er aktiv`() {
        // Act
        val aktivBehandling = behandlingRepository.hentAktivBehandling(behandling.id)

        // Assert
        assertTrue(aktivBehandling.aktiv)
    }

    @Test
    fun `finnBehandlinger - skal finne behandlinger tilknyttet fagsakId`() {
        // Act
        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        // Assert
        assertEquals(1, behandlinger.size)
    }

    @Test
    fun `finnBehandlinger - skal returnere tom liste dersom det ikke finnes behandlinger tilknyttet fagsakId`() {
        // Act
        val behandlinger = behandlingRepository.finnBehandlinger(404L)

        // Assert
        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `findByFagsakAndAktiv - skal finne aktiv behandling tilknyttet fagsakId`() {
        // Act
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)

        // Assert
        assertNotNull(behandling)
        assertEquals(fagsak.id, behandling?.fagsak?.id)
    }

    @Test
    fun `findByFagsakAndAktiv - skal returnere null dersom det ikke finnes en aktiv behandling tilknyttet fagsakId`() {
        // Arrange
        behandlingRepository.saveAndFlush(behandling.also { it.aktiv = false })

        // Act
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)

        // Assert
        assertNull(behandling)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal finne aktiv og åpen behandling tilknyttet fagsakId`() {
        // Act
        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id)

        // Assert
        assertNotNull(behandling)
        assertEquals(fagsak.id, behandling?.fagsak?.id)
        assertTrue(behandling!!.aktiv)
        assertTrue(behandling.status !== BehandlingStatus.AVSLUTTET)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal returnere null dersom aktiv behandling tilknyttet fagsakId er avsluttet`() {
        // Arrange
        behandlingRepository.saveAndFlush(behandling.also { it.status = BehandlingStatus.AVSLUTTET })

        // Act
        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id)

        // Assert
        assertNull(behandling)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal returnere null dersom åpen behandling tilknyttet fagsakId ikke er aktiv`() {
        // Arrange
        behandlingRepository.saveAndFlush(behandling.also { it.aktiv = false })

        // Act
        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id)

        // Assert
        assertNull(behandling)
    }

    @Test
    fun `finnIverksatteBehandlinger - skal returnere alle behandlinger som har tilkjentytelse med utbetalingsoppdrag i fagsak`() {
        // Arrange
        lagTilkjentYtelse("utbetalingsoppdrag")

        // Act
        val behandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsak.id)

        // Assert
        assertThat(behandlinger.size, Is(1))
        assertThat(behandlinger.single().id, Is(behandling.id))
    }

    @Test
    fun `finnIverksatteBehandlinger - skal returnere tom liste dersom det ikke er noen behandliner som har tilkjentytelse med utbetalingsoppdrag i fagsak`() {
        // Arrange
        lagTilkjentYtelse(null)

        // Act
        val behandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsak.id)

        // Assert
        assertThat(behandlinger.size, Is(0))
    }

    @Test
    fun `finnBehandlingerSomHolderPåÅIverksettes - skal returnere alle behandlinger som har status 'IVERKSETTER_VEDTAK'`() {
        // Arrange
        behandlingRepository.saveAndFlush(behandling.also { it.status = BehandlingStatus.IVERKSETTER_VEDTAK })

        // Act
        val behandlinger = behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsak.id)

        // Assert
        assertThat(behandlinger.size, Is(1))
        assertThat(behandlinger.single().id, Is(behandling.id))
    }

    @Test
    fun `finnBehandlingerSomHolderPåÅIverksettes - skal returnere tom liste dersom status er ulik 'IVERKSETTER_VEDTAK'`() {
        // Arrange
        behandlingRepository.saveAndFlush(behandling.also { it.status = BehandlingStatus.UTREDES })

        // Act
        val behandlinger = behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsak.id)

        // Assert
        assertThat(behandlinger.size, Is(0))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStegStatus::class,
        names = ["KLAR", "VENTER"],
    )
    fun `finnBehandlingerSentTilGodkjenning - skal returnere alle behandlinger som står på steget BESLUTTE_VEDTAK og har status 'KLAR' eller 'VENTER'`(
        behandlingStegStatus: BehandlingStegStatus,
    ) {
        // Arrange
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK,
                behandlingStegStatus = behandlingStegStatus,
            ),
        )
        behandlingRepository.saveAndFlush(behandling)

        // Act
        val behandlinger = behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsak.id)

        // Assert
        assertThat(behandlinger.size, Is(1))
        assertThat(behandlinger.single().id, Is(behandling.id))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStegStatus::class,
        names = ["KLAR", "VENTER"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `finnBehandlingerSentTilGodkjenning - skal returnere tom liste når behandling står på steget BESLUTTE_VEDTAK og har status som ikke er 'KLAR' eller 'VENTER'`(
        behandlingStegStatus: BehandlingStegStatus,
    ) {
        // Arrange
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK,
                behandlingStegStatus = behandlingStegStatus,
            ),
        )
        behandlingRepository.saveAndFlush(behandling)

        // Act
        val behandlinger = behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsak.id)

        // Assert
        assertThat(behandlinger.size, Is(0))
    }
}
