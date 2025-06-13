package no.nav.familie.ks.sak.kjerne.endretutbetaling.domene

import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelKtTest {
    @Nested
    inner class FraEndretUtbetalingAndelRequestDtoTest {
        @Test
        fun `skal oppdatere EndretUtbetalingAndel`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    personIdent = "12345678903",
                    prosent = BigDecimal(100),
                    fom = YearMonth.now().minusMonths(1),
                    tom = YearMonth.now(),
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                    søknadstidspunkt = LocalDate.now().minusMonths(1),
                    begrunnelse = "en annen begrunnelse",
                    erEksplisittAvslagPåSøknad = false,
                )

            val personer = setOf(lagPerson(aktør = randomAktør()))

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    personer = personer,
                    prosent = BigDecimal(50),
                    periodeFom = YearMonth.now().minusMonths(2),
                    periodeTom = YearMonth.now().plusMonths(2),
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    søknadstidspunkt = LocalDate.now().minusMonths(2),
                    begrunnelse = "en begrunnelse",
                )

            // Act
            val oppdatertEndretUtbetalingAndel =
                endretUtbetalingAndel.fraEndretUtbetalingAndelRequestDto(
                    endretUtbetalingAndelRequestDto,
                    personer,
                )

            // Assert
            assertThat(oppdatertEndretUtbetalingAndel.id).isEqualTo(endretUtbetalingAndel.id)
            assertThat(oppdatertEndretUtbetalingAndel.behandlingId).isEqualTo(endretUtbetalingAndel.id)
            assertThat(oppdatertEndretUtbetalingAndel.personer).isEqualTo(personer)
            assertThat(oppdatertEndretUtbetalingAndel.prosent).isEqualTo(endretUtbetalingAndelRequestDto.prosent)
            assertThat(oppdatertEndretUtbetalingAndel.fom).isEqualTo(endretUtbetalingAndelRequestDto.fom)
            assertThat(oppdatertEndretUtbetalingAndel.tom).isEqualTo(endretUtbetalingAndelRequestDto.tom)
            assertThat(oppdatertEndretUtbetalingAndel.årsak).isEqualTo(endretUtbetalingAndelRequestDto.årsak)
            assertThat(oppdatertEndretUtbetalingAndel.søknadstidspunkt).isEqualTo(endretUtbetalingAndelRequestDto.søknadstidspunkt)
            assertThat(oppdatertEndretUtbetalingAndel.begrunnelse).isEqualTo(endretUtbetalingAndelRequestDto.begrunnelse)
            assertThat(oppdatertEndretUtbetalingAndel.vedtaksbegrunnelser).containsOnly()
            assertThat(oppdatertEndretUtbetalingAndel.erEksplisittAvslagPåSøknad).isEqualTo(endretUtbetalingAndelRequestDto.erEksplisittAvslagPåSøknad)
        }
    }
}
