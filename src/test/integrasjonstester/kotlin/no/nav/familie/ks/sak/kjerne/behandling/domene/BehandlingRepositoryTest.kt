package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
}
