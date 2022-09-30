package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.vedtak.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.vedtak.domene.VedtakRepository
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.hamcrest.CoreMatchers.`is` as Is

internal class VedtakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun beforeEach() {
        fagsak = lagreFagsak()
        behandling = lagreBehandling(fagsak)
    }

    @Test
    fun `findByBehandlingAndAktiv - skal kaste EmptyResultDataAccessException hvis det ikke finnes aktiv vedtak for behandling`() {
        val vedtak = Vedtak(behandling = behandling, aktiv = false)
        vedtakRepository.saveAndFlush(vedtak)

        val feil = assertThrows<EmptyResultDataAccessException> { vedtakRepository.findByBehandlingAndAktiv(behandling.id) }

        assertThat(feil.message, Is("Result must not be null"))
    }

    @Test
    fun `findByBehandlingAndAktiv - skal returnere vedtak hvis det finnes aktiv vedtak for behandling`() {
        val vedtak = Vedtak(behandling = behandling, aktiv = true)
        vedtakRepository.saveAndFlush(vedtak)

        val hentetVedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)

        assertThat(hentetVedtak.behandling, Is(behandling))
    }

    @Test
    fun `findByBehandlingAndAktivOptional - skal returnere null hvis det ikke finnes aktiv vedtak for behandling`() {
        val vedtak = Vedtak(behandling = behandling, aktiv = false)
        vedtakRepository.saveAndFlush(vedtak)

        val hentetVedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)

        assertThat(hentetVedtak, Is(nullValue()))
    }

    @Test
    fun `findByBehandlingAndAktivOptional - skal returnere vedtak hvis det finnes aktiv vedtak for behandling`() {
        val vedtak = Vedtak(behandling = behandling, aktiv = true)
        vedtakRepository.saveAndFlush(vedtak)

        val hentetVedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)!!

        assertThat(hentetVedtak.behandling, Is(behandling))
    }

    @Test
    fun `finnVedtakForBehandling - skal returnere tom liste hvis det ikke finnes noen vedtak for behandling`() {
        val hentetVedtaker = vedtakRepository.finnVedtakForBehandling(behandling.id)

        assertThat(hentetVedtaker.size, Is(0))
    }

    @Test
    fun `finnVedtakForBehandling - skal returnere liste med alle vedtak hvis det finnes for behandling`() {
        val vedtak1 = Vedtak(behandling = behandling, aktiv = false)
        val vedtak2 = Vedtak(behandling = behandling, aktiv = true)

        vedtakRepository.saveAllAndFlush(listOf(vedtak1, vedtak2))

        val hentetVedtaker = vedtakRepository.finnVedtakForBehandling(behandling.id)

        assertThat(hentetVedtaker.size, Is(2))
        assertThat(hentetVedtaker.map { it.aktiv }, containsInAnyOrder(false, true))
    }

    private fun lagreFagsak(): Fagsak {
        val aktør = aktørRepository.saveAndFlush(randomAktør())
        return fagsakRepository.saveAndFlush(lagFagsak(aktør))
    }

    private fun lagreBehandling(fagsak: Fagsak): Behandling = behandlingRepository.saveAndFlush(lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))
}
