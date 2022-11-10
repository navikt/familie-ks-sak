package no.nav.familie.ks.sak.kjerne.behandling.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
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
        opprettSøkerFagsakOgBehandling()
    }

    @Test
    fun `hentBehandling - skal finne behandling med behandlingsId`() {
        val hentetBehandling = behandlingRepository.hentBehandling(behandling.id)

        assertEquals(behandling.id, hentetBehandling.id)
    }

    @Test
    fun `hentAktivBehandling - skal finne behandling med behandlingsId som er aktiv`() {
        val aktivBehandling = behandlingRepository.hentAktivBehandling(behandling.id)

        assertTrue(aktivBehandling.aktiv)
    }

    @Test
    fun `finnBehandlinger - skal finne behandlinger tilknyttet fagsakId`() {
        val behandlinger = behandlingRepository.finnBehandlinger(fagsak.id)

        assertEquals(1, behandlinger.size)
    }

    @Test
    fun `finnBehandlinger - skal returnere tom liste dersom det ikke finnes behandlinger tilknyttet fagsakId`() {
        val behandlinger = behandlingRepository.finnBehandlinger(404L)

        assertEquals(0, behandlinger.size)
    }

    @Test
    fun `findByFagsakAndAktiv - skal finne aktiv behandling tilknyttet fagsakId`() {
        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)

        assertNotNull(behandling)
        assertEquals(fagsak.id, behandling?.fagsak?.id)
    }

    @Test
    fun `findByFagsakAndAktiv - skal returnere null dersom det ikke finnes en aktiv behandling tilknyttet fagsakId`() {
        behandlingRepository.saveAndFlush(behandling.also { it.aktiv = false })

        val behandling = behandlingRepository.findByFagsakAndAktiv(fagsak.id)

        assertNull(behandling)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal finne aktiv og åpen behandling tilknyttet fagsakId`() {
        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id)

        assertNotNull(behandling)
        assertEquals(fagsak.id, behandling?.fagsak?.id)
        assertTrue(behandling!!.aktiv)
        assertTrue(behandling.status !== BehandlingStatus.AVSLUTTET)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal returnere null dersom aktiv behandling tilknyttet fagsakId er avsluttet`() {
        behandlingRepository.saveAndFlush(behandling.also { it.status = BehandlingStatus.AVSLUTTET })

        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id)

        assertNull(behandling)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal returnere null dersom åpen behandling tilknyttet fagsakId ikke er aktiv`() {
        behandlingRepository.saveAndFlush(behandling.also { it.aktiv = false })

        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id)

        assertNull(behandling)
    }

    @Test
    fun `finnIverksatteBehandlinger - skal returnere alle behandlinger som har tilkjentytelse med utbetalingsoppdrag i fagsak`() {
        lagTilkjentYtelse("utbetalingsoppdrag")

        val behandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsak.id)

        assertThat(behandlinger.size, Is(1))
        assertThat(behandlinger.single().id, Is(behandling.id))
    }

    @Test
    fun `finnIverksatteBehandlinger - skal returnere tom liste dersom det ikke er noen behandliner som har tilkjentytelse med utbetalingsoppdrag i fagsak`() {
        lagTilkjentYtelse(null)

        val behandlinger = behandlingRepository.finnIverksatteBehandlinger(fagsak.id)

        assertThat(behandlinger.size, Is(0))
    }

    @Test
    fun `finnBehandlingerSomHolderPåÅIverksettes - skal returnere alle behandlinger som har status 'IVERKSETTER_VEDTAK'`() {
        behandlingRepository.saveAndFlush(behandling.also { it.status = BehandlingStatus.IVERKSETTER_VEDTAK })
        val behandlinger = behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsak.id)
        assertThat(behandlinger.size, Is(1))
        assertThat(behandlinger.single().id, Is(behandling.id))
    }

    @Test
    fun `finnBehandlingerSomHolderPåÅIverksettes - skal returnere tom liste dersom status er ulik 'IVERKSETTER_VEDTAK'`() {
        behandlingRepository.saveAndFlush(behandling.also { it.status = BehandlingStatus.UTREDES })
        val behandlinger = behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsak.id)
        assertThat(behandlinger.size, Is(0))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStegStatus::class,
        names = ["KLAR", "VENTER"]
    )
    fun `finnBehandlingerSentTilGodkjenning - skal returnere alle behandlinger som står på steget BESLUTTE_VEDTAK og har status 'KLAR' eller 'VENTER'`(
        behandlingStegStatus: BehandlingStegStatus
    ) {
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK,
                behandlingStegStatus = behandlingStegStatus
            )
        )
        behandlingRepository.saveAndFlush(behandling)
        val behandlinger = behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsak.id)
        assertThat(behandlinger.size, Is(1))
        assertThat(behandlinger.single().id, Is(behandling.id))
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStegStatus::class,
        names = ["KLAR", "VENTER"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `finnBehandlingerSentTilGodkjenning - skal returnere tom liste når behandling står på steget BESLUTTE_VEDTAK og har status som ikke er 'KLAR' eller 'VENTER'`(
        behandlingStegStatus: BehandlingStegStatus
    ) {
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandling = behandling,
                behandlingSteg = BehandlingSteg.BESLUTTE_VEDTAK,
                behandlingStegStatus = behandlingStegStatus
            )
        )
        behandlingRepository.saveAndFlush(behandling)
        val behandlinger = behandlingRepository.finnBehandlingerSendtTilGodkjenning(fagsak.id)
        assertThat(behandlinger.size, Is(0))
    }
}
