package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeRepository
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class VedtaksperiodeHentOgPersisterServiceTest {

    @MockK
    private lateinit var vedtaksperiodeRepository: VedtaksperiodeRepository

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @InjectMockKs
    private lateinit var vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService

    @Test
    fun `hentVedtaksperiodeThrows - skal kaste feil dersom det ikke finnes noe vedtaksperiode med gitt id`() {
        every { vedtaksperiodeRepository.finnVedtaksperiode(404) } returns null

        val exception = assertThrows<RuntimeException> {
            vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(404)
        }

        assertThat(exception.message, Is("Fant ingen vedtaksperiode med id 404"))
    }

    @Test
    fun `hentVedtaksperiodeThrows - skal returnere vedtaksperiode med gitt id dersom det finnes`() {
        val mocketVedtaksperiode = mockk<VedtaksperiodeMedBegrunnelser>()
        every { vedtaksperiodeRepository.finnVedtaksperiode(404) } returns mocketVedtaksperiode

        val vedtaksperiode = vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(404)

        assertThat(vedtaksperiode, Is(mocketVedtaksperiode))
    }

    @Test
    fun `slettVedtaksperioderFor - skal slette vedtaksperioder for vedtak`() {
        val mocketVedtak = mockk<Vedtak>()
        every { vedtaksperiodeRepository.slettVedtaksperioderForVedtak(mocketVedtak) } just runs

        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(vedtak = mocketVedtak)

        verify(exactly = 1) { vedtaksperiodeRepository.slettVedtaksperioderForVedtak(mocketVedtak) }
    }

    @Test
    fun `finnVedtaksperioderFor - skal returnere vedtaksperioder for vedtak`() {
        every { vedtaksperiodeRepository.finnVedtaksperioderForVedtak(200) } returns listOf(mockk())

        val vedtaksperioder = vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(200)

        assertThat(vedtaksperioder.size, Is(1))
    }

    @Test
    fun `slettVedtaksperioderFor - skal slette vedtaksperioder for behandling`() {
        val mocketVedtak = mockk<Vedtak>()
        every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns mocketVedtak
        every { vedtaksperiodeRepository.slettVedtaksperioderForVedtak(mocketVedtak) } just runs

        vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(behandlingId = 200)

        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(200) }
        verify(exactly = 1) { vedtaksperiodeRepository.slettVedtaksperioderForVedtak(mocketVedtak) }
    }
}
