package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.vedtak.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
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

    private lateinit var søker: Aktør
    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun beforeEach() {
        søker = lagreAktør(randomAktør())
        fagsak = lagreFagsak(lagFagsak(søker))
        behandling = lagreBehandling(lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))
    }

    @Test
    fun `findByBehandlingAndAktiv - skal kaste EmptyResultDataAccessException hvis det ikke finnes aktiv vedtak for behandling`() {
        val vedtak = Vedtak(behandling = behandling, aktiv = false)
        vedtakRepository.saveAndFlush(vedtak)

        val feil =
            assertThrows<EmptyResultDataAccessException> { vedtakRepository.findByBehandlingAndAktiv(behandling.id) }

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
}
