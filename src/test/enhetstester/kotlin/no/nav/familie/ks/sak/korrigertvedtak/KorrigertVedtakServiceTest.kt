package no.nav.familie.ks.sak.korrigertvedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

internal class KorrigertVedtakServiceTest {
    private val korrigertVedtakRepository = mockk<KorrigertVedtakRepository>()
    private val loggService = mockk<LoggService>()

    private val korrigertVedtakService = KorrigertVedtakService(korrigertVedtakRepository, loggService)

    @Test
    fun `finnAktivtKorrigertVedtakPåBehandling skal hente aktivt korrigert vedtak fra repository hvis det finnes`() {
        val behandling = lagBehandling()
        val korrigertVedtak = lagKorrigertVedtak(behandling)

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) } returns korrigertVedtak

        val hentetKorrigertVedtak =
            korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandling.id)
                ?: fail("korrigert vedtak ikke hentet riktig")

        assertThat(hentetKorrigertVedtak.behandling.id, Is(behandling.id))
        assertThat(hentetKorrigertVedtak.aktiv, Is(true))

        verify(exactly = 1) { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) }
    }

    @Test
    fun `lagreKorrigertVedtak skal lagre korrigert vedtak på behandling og logg på dette`() {
        val behandling = lagBehandling()
        val korrigertVedtak = lagKorrigertVedtak(behandling)

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) } returns null
        every { korrigertVedtakRepository.save(korrigertVedtak) } returns korrigertVedtak
        every { loggService.opprettKorrigertVedtakLogg(behandling, any()) } returns Unit

        val lagretKorrigertVedtak =
            korrigertVedtakService.lagreKorrigertVedtakOgDeaktiverGamle(korrigertVedtak)

        assertThat(lagretKorrigertVedtak.behandling.id, Is(behandling.id))

        verify(exactly = 1) { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) }
        verify(exactly = 1) { korrigertVedtakRepository.save(korrigertVedtak) }
        verify(exactly = 1) {
            loggService.opprettKorrigertVedtakLogg(
                behandling,
                korrigertVedtak,
            )
        }
    }

    @Test
    fun `lagreKorrigertVedtak skal sette og lagre forrige korrigert vedtak til inaktivt hvis det finnes tidligere korrigering`() {
        val behandling = lagBehandling()
        val forrigeKorrigering = mockk<KorrigertVedtak>(relaxed = true)
        val korrigertVedtak = lagKorrigertVedtak(behandling, vedtaksdato = LocalDate.now().minusDays(3))

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(any()) } returns forrigeKorrigering
        every { korrigertVedtakRepository.saveAndFlush(forrigeKorrigering) } returns korrigertVedtak
        every { korrigertVedtakRepository.save(korrigertVedtak) } returns korrigertVedtak
        every { loggService.opprettKorrigertVedtakLogg(any(), any()) } returns Unit

        korrigertVedtakService.lagreKorrigertVedtakOgDeaktiverGamle(korrigertVedtak)

        verify(exactly = 1) { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(any()) }
        verify(exactly = 1) { forrigeKorrigering setProperty "aktiv" value false }
        verify(exactly = 1) { korrigertVedtakRepository.saveAndFlush(forrigeKorrigering) }
        verify(exactly = 1) { korrigertVedtakRepository.save(korrigertVedtak) }
    }

    @Test
    fun `settKorrigertVedtakPåBehandlingTilInaktiv skal sette korrigert vedtak til inaktivt hvis det finnes`() {
        val behandling = lagBehandling()
        val korrigertVedtak = mockk<KorrigertVedtak>(relaxed = true)

        every { korrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(any()) } returns korrigertVedtak
        every { loggService.opprettKorrigertVedtakLogg(any(), any()) } returns Unit

        korrigertVedtakService.settKorrigertVedtakPåBehandlingTilInaktiv(behandling)

        verify(exactly = 1) { korrigertVedtak setProperty "aktiv" value false }
        verify(exactly = 1) {
            loggService.opprettKorrigertVedtakLogg(
                any(),
                korrigertVedtak,
            )
        }
    }

    fun lagKorrigertVedtak(
        behandling: Behandling,
        vedtaksdato: LocalDate = LocalDate.now().minusDays(6),
        begrunnelse: String? = null,
        aktiv: Boolean = true,
    ) = KorrigertVedtak(
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        begrunnelse = begrunnelse,
        aktiv = aktiv,
    )
}
