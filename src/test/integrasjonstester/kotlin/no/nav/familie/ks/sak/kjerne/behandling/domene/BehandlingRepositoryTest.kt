package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.behandling.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun beforeAll() {
        fagsak = lagreFagsak()
        behandling = lagreBehandling(fagsak)
    }

    @Test
    fun `hentBehandling - skal finne behandling med behandlingsId`() {
        val hentetBehandling = behandlingRepository.hentBehandling(behandling.id)

        assertNotNull(hentetBehandling)
        assertEquals(behandling.id, hentetBehandling.id)
    }

    @Test
    fun `hentAktivBehandling - skal finne behandling med behandlingsId som er aktiv`() {
        val aktivBehandling = behandlingRepository.hentAktivBehandling(behandling.id)

        assertNotNull(aktivBehandling)
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
        val nyFagsak = lagreFagsak()
        val nyBehandling = lagreBehandling(nyFagsak)
        behandlingRepository.saveAndFlush(nyBehandling.copy(aktiv = false))

        val behandling = behandlingRepository.findByFagsakAndAktiv(nyFagsak.id)

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
        val nyFagsak = lagreFagsak()
        val nyBehandling = lagreBehandling(nyFagsak)
        behandlingRepository.saveAndFlush(nyBehandling.copy(status = BehandlingStatus.AVSLUTTET))

        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(nyFagsak.id)

        assertNull(behandling)
    }

    @Test
    fun `findByFagsakAndAktivAndOpen - skal returnere null dersom åpen behandling tilknyttet fagsakId ikke er aktiv`() {
        val nyFagsak = lagreFagsak()
        val nyBehandling = lagreBehandling(nyFagsak)
        behandlingRepository.saveAndFlush(nyBehandling.copy(aktiv = false))

        val behandling = behandlingRepository.findByFagsakAndAktivAndOpen(nyFagsak.id)

        assertNull(behandling)
    }

    private fun lagreFagsak(): Fagsak {
        val aktør = aktørRepository.saveAndFlush(randomAktør())
        return fagsakRepository.saveAndFlush(lagFagsak(aktør))
    }

    private fun lagreBehandling(fagsak: Fagsak): Behandling {
        return behandlingRepository.saveAndFlush(lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))
    }
}
