package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.vedtaksperiode.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeRepository
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.hamcrest.CoreMatchers.`is` as Is

class VedtaksperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vedtaksperiodeRepository: VedtaksperiodeRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling()
        lagVedtak()
    }

    @Test
    fun `finnVedtaksperiode skal returnere null dersom det ikke finnes noen vedtaksperiode med lik id`() {
        val vedtaksperiode = vedtaksperiodeRepository.finnVedtaksperiode(404)

        assertThat(vedtaksperiode, Is(nullValue()))
    }

    @Test
    fun `finnVedtaksperiode skal returnere vedtaksperiode dersom det finnes vedtaksperiode med lik id`() {
        val lagretVedtaksperiode = opprettOgLagVedtaksperiode()

        val vedtaksperiode = vedtaksperiodeRepository.finnVedtaksperiode(lagretVedtaksperiode.id)!!

        assertThat(lagretVedtaksperiode.id, Is(vedtaksperiode.id))
    }

    @Test
    @Transactional
    fun `slettVedtaksperioderFor skal slette vedtakperioder for en gitt vedtak`() {
        val vedtaksperiodeId = opprettOgLagVedtaksperiode().id

        vedtaksperiodeRepository.slettVedtaksperioderForVedtak(vedtak)

        val vedtaksperiode = vedtaksperiodeRepository.finnVedtaksperiode(vedtaksperiodeId)

        assertThat(vedtaksperiode, Is(nullValue()))
    }

    @Test
    fun `finnVedtaksperioderForVedtak skal returnere vedtaksperioder for en gitt vedtak`() {
        opprettOgLagVedtaksperiode()

        val vedtaksperioderForVedtak = vedtaksperiodeRepository.finnVedtaksperioderForVedtak(vedtak.id)

        assertThat(vedtaksperioderForVedtak.size, Is(1))
    }

    private fun opprettOgLagVedtaksperiode(): VedtaksperiodeMedBegrunnelser {
        val vedtaksperiodeMedBegrunnelser =
            VedtaksperiodeMedBegrunnelser(vedtak = vedtak, type = Vedtaksperiodetype.OPPHØR)

        return vedtaksperiodeRepository.saveAndFlush(vedtaksperiodeMedBegrunnelser)
    }
}
