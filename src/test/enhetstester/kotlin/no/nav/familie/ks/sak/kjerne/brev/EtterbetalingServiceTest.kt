package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingService
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.lagKorrigertEtterbetaling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EtterbetalingServiceTest {
    private val mockedKorrigertEtterbetalingService: KorrigertEtterbetalingService = mockk()
    private val mocekdSimuleringService: SimuleringService = mockk()
    private val etterbetalingService =
        EtterbetalingService(
            mockedKorrigertEtterbetalingService,
            mocekdSimuleringService,
        )

    @Test
    fun `skal bruke korrigert etterbetaling`() {
        // Arrange
        val vedtak = lagVedtak()

        val korrigertEtterbetaling =
            lagKorrigertEtterbetaling(
                behandling = vedtak.behandling,
                beløp = 2_000,
            )

        every {
            mockedKorrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns korrigertEtterbetaling

        every {
            mocekdSimuleringService.hentEtterbetaling(
                behandlingId = vedtak.behandling.id,
            )
        } returns BigDecimal.valueOf(0L)

        // Act
        val etterbetaling =
            etterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )

        // Assert
        assertThat(etterbetaling?.etterbetalingsbelop).containsOnly(formaterBeløp(2000))
    }

    @Test
    fun `skal bruke etterbetaling fra simulering`() {
        // Arrange
        val vedtak = lagVedtak()

        every {
            mockedKorrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns null

        every {
            mocekdSimuleringService.hentEtterbetaling(
                behandlingId = vedtak.behandling.id,
            )
        } returns BigDecimal.valueOf(1000L)

        // Act
        val etterbetaling =
            etterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )

        // Assert
        assertThat(etterbetaling?.etterbetalingsbelop).containsOnly(formaterBeløp(1000))
    }

    @Test
    fun `skal returnere null om etterbetalingsbeløpet er 0`() {
        // Arrange
        val vedtak = lagVedtak()

        every {
            mockedKorrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns null

        every {
            mocekdSimuleringService.hentEtterbetaling(
                behandlingId = vedtak.behandling.id,
            )
        } returns BigDecimal.valueOf(0L)

        // Act
        val etterbetaling =
            etterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )

        // Assert
        assertThat(etterbetaling).isNull()
    }
}
