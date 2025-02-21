package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType.REVURDERING
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.ENDRET_OG_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.brev.GenererBrevService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

class VedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val genererBrevService = mockk<GenererBrevService>()
    private val behandlingService = mockk<BehandlingService>()

    private val vedtakService = VedtakService(vedtakRepository, vedtaksperiodeService, genererBrevService, behandlingService)

    @Test
    fun `hentVedtak - skal hente vedtak fra VedtakRepository`() {
        every { vedtakRepository.hentVedtak(1) } returns mockk()

        val hentetVedtak = vedtakService.hentVedtak(1)

        Assertions.assertNotNull(hentetVedtak)
        verify(exactly = 1) { vedtakRepository.hentVedtak(1) }
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "LOVENDRING_2024", "IVERKSETTE_KA_VEDTAK"], mode = EnumSource.Mode.EXCLUDE)
    fun `oppdaterVedtakMedDatoOgStønadsbrev - skal oppdatere vedtak med dato og stønadsbrev`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling = lagBehandling(opprettetÅrsak = behandlingÅrsak, resultat = ENDRET_OG_OPPHØRT)
        val vedtak = lagVedtak(behandling)
        val brev = "brev".toByteArray()

        mockkStatic(LocalDateTime::class)

        every { genererBrevService.genererBrevForBehandling(any()) } returns brev
        every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns vedtak
        every { vedtakRepository.saveAndFlush(any()) } answers { firstArg() }
        every { behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(any()) } returns false
        every { LocalDateTime.now() } returns LocalDateTime.of(2024, 1, 1, 0, 0)

        val oppdatertVedtak = vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(behandling)

        assertThat(oppdatertVedtak.stønadBrevPdf, Is(brev))
        assertThat(oppdatertVedtak.vedtaksdato, Is(LocalDateTime.of(2024, 1, 1, 0, 0)))

        verify(exactly = 1) { genererBrevService.genererBrevForBehandling(vedtak) }
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { vedtakRepository.saveAndFlush(oppdatertVedtak) }

        unmockkStatic(LocalDateTime::class)
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingÅrsak::class, names = ["LOVENDRING_2024", "SATSENDRING"])
    fun `oppdaterVedtakMedDatoOgStønadsbrev - skal oppdatere vedtak med dato, men ikke stønadsbrev`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        val behandling = lagBehandling(opprettetÅrsak = behandlingÅrsak, resultat = INNVILGET, type = REVURDERING)
        val vedtak = lagVedtak(behandling)

        mockkStatic(LocalDateTime::class)

        every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns vedtak
        every { vedtakRepository.saveAndFlush(any()) } answers { firstArg() }
        every { behandlingService.erLovendringOgFremtidigOpphørOgHarFlereAndeler(any()) } returns false
        every { LocalDateTime.now() } returns LocalDateTime.of(2024, 1, 1, 0, 0)

        val oppdatertVedtak = vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(behandling)

        assertThat(oppdatertVedtak.stønadBrevPdf, IsNull())
        assertThat(oppdatertVedtak.vedtaksdato, Is(LocalDateTime.of(2024, 1, 1, 0, 0)))

        verify(exactly = 0) { genererBrevService.genererBrevForBehandling(vedtak) }
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { vedtakRepository.saveAndFlush(oppdatertVedtak) }

        unmockkStatic(LocalDateTime::class)
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingSteg::class, names = ["BESLUTTE_VEDTAK", "REGISTRERE_PERSONGRUNNLAG"], mode = EnumSource.Mode.EXCLUDE)
    fun `opprettOgInitierNyttVedtakForBehandling - skal kaste feil hvis steg ikke er BESLUTTE_VEDTAK eller REGISTRERE_PERSONGRUNNLAG`(
        behandlingSteg: BehandlingSteg,
    ) {
        val behandling = mockk<Behandling>()
        every { behandling.steg } returns behandlingSteg

        val feil = assertThrows<Feil> { vedtakService.opprettOgInitierNyttVedtakForBehandling(behandling, false) }

        assertThat(feil.message, Is("Forsøker å initiere vedtak på steg ${behandlingSteg.name}"))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingSteg::class, names = ["BESLUTTE_VEDTAK", "REGISTRERE_PERSONGRUNNLAG"])
    fun `opprettOgInitierNyttVedtakForBehandling - skal lagre ny vedtak og deaktivere gamle hvis steg er BESLUTTE_VEDTAK eller REGISTRERE_PERSONGRUNNLAG`(
        behandlingSteg: BehandlingSteg,
    ) {
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
