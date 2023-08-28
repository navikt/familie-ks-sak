package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class VedtakServiceTest {

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    @InjectMockKs
    private lateinit var vedtakService: VedtakService

    @Test
    fun `hentVedtak - skal hente vedtak fra VedtakRepository`() {
        every { vedtakRepository.hentVedtak(1) } returns mockk()

        val hentetVedtak = vedtakService.hentVedtak(1)

        Assertions.assertNotNull(hentetVedtak)
        verify(exactly = 1) { vedtakRepository.hentVedtak(1) }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingSteg::class, names = ["BESLUTTE_VEDTAK", "REGISTRERE_PERSONGRUNNLAG"], mode = EnumSource.Mode.EXCLUDE)
    fun `opprettOgInitierNyttVedtakForBehandling - skal kaste feil hvis steg ikke er BESLUTTE_VEDTAK eller REGISTRERE_PERSONGRUNNLAG`(behandlingSteg: BehandlingSteg) {
        val behandling = mockk<Behandling>()
        every { behandling.steg } returns behandlingSteg

        val feil = assertThrows<Feil> { vedtakService.opprettOgInitierNyttVedtakForBehandling(behandling, false) }

        assertThat(feil.message, Is("Forsøker å initiere vedtak på steg ${behandlingSteg.name}"))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingSteg::class, names = ["BESLUTTE_VEDTAK", "REGISTRERE_PERSONGRUNNLAG"])
    fun `opprettOgInitierNyttVedtakForBehandling - skal lagre ny vedtak og deaktivere gamle hvis steg er BESLUTTE_VEDTAK eller REGISTRERE_PERSONGRUNNLAG`(behandlingSteg: BehandlingSteg) {
        val behandling = mockk<Behandling>(relaxed = true)
        val eksisterendeVedtak = mockk<Vedtak>(relaxed = true)
        val slot = slot<Vedtak>()

        every { behandling.steg } returns behandlingSteg

        every { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) } returns eksisterendeVedtak
        every { vedtakRepository.saveAndFlush(eksisterendeVedtak) } returns eksisterendeVedtak
        every { vedtakRepository.save(capture(slot)) } returns mockk(relaxed = true)

        vedtakService.opprettOgInitierNyttVedtakForBehandling(behandling, false)

        val lagretVedtak = slot.captured

        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { eksisterendeVedtak setProperty "aktiv" value false }
        verify(exactly = 1) { vedtakRepository.save(lagretVedtak) }

        assertThat(lagretVedtak.behandling, Is(behandling))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingSteg::class, names = ["BESLUTTE_VEDTAK", "REGISTRERE_PERSONGRUNNLAG"])
    fun `opprettOgInitierNyttVedtakForBehandling - skal kopiere over vedtaksperioder dersom det eksisterer gammmelt vedtak og kopierOverVedtaksperioder er satt til true`(
        behandlingSteg: BehandlingSteg,
    ) {
        val behandling = mockk<Behandling>(relaxed = true)
        val eksisterendeVedtak = mockk<Vedtak>(relaxed = true)
        val slot = slot<Vedtak>()

        every { behandling.steg } returns behandlingSteg

        every { vedtaksperiodeService.kopierOverVedtaksperioder(any(), any()) } returns mockk()
        every { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) } returns eksisterendeVedtak
        every { vedtakRepository.saveAndFlush(eksisterendeVedtak) } returns eksisterendeVedtak
        every { vedtakRepository.save(capture(slot)) } returns mockk(relaxed = true)

        vedtakService.opprettOgInitierNyttVedtakForBehandling(behandling, true)

        val lagretVedtak = slot.captured

        verify(exactly = 1) { vedtaksperiodeService.kopierOverVedtaksperioder(any(), any()) }
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { eksisterendeVedtak setProperty "aktiv" value false }
        verify(exactly = 1) { vedtakRepository.save(lagretVedtak) }

        assertThat(lagretVedtak.behandling, Is(behandling))
    }
}
