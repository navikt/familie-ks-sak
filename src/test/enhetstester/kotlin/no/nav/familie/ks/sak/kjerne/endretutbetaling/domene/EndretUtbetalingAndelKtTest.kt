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
                    årsak = Årsak.ENDRE_MOTTAKER,
                    avtaletidspunktDeltBosted = LocalDate.now(),
                    søknadstidspunkt = LocalDate.now().minusMonths(1),
                    begrunnelse = "en annen begrunnelse",
                    erEksplisittAvslagPåSøknad = false,
                )

            val person = lagPerson(aktør = randomAktør())

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    person = person,
                    prosent = BigDecimal(50),
                    periodeFom = YearMonth.now().minusMonths(2),
                    periodeTom = YearMonth.now().plusMonths(2),
                    årsak = Årsak.DELT_BOSTED,
                    avtaletidspunktDeltBosted = LocalDate.now().minusMonths(2),
                    søknadstidspunkt = LocalDate.now().minusMonths(2),
                    begrunnelse = "en begrunnelse",
                )

            // Act
            val oppdatertEndretUtbetalingAndel =
                endretUtbetalingAndel.fraEndretUtbetalingAndelRequestDto(
                    endretUtbetalingAndelRequestDto,
                    person,
                )

            // Assert
            assertThat(oppdatertEndretUtbetalingAndel.id).isEqualTo(endretUtbetalingAndel.id)
            assertThat(oppdatertEndretUtbetalingAndel.behandlingId).isEqualTo(endretUtbetalingAndel.id)
            assertThat(oppdatertEndretUtbetalingAndel.person).isEqualTo(person)
            assertThat(oppdatertEndretUtbetalingAndel.prosent).isEqualTo(endretUtbetalingAndelRequestDto.prosent)
            assertThat(oppdatertEndretUtbetalingAndel.fom).isEqualTo(endretUtbetalingAndelRequestDto.fom)
            assertThat(oppdatertEndretUtbetalingAndel.tom).isEqualTo(endretUtbetalingAndelRequestDto.tom)
            assertThat(oppdatertEndretUtbetalingAndel.årsak).isEqualTo(endretUtbetalingAndelRequestDto.årsak)
            assertThat(oppdatertEndretUtbetalingAndel.avtaletidspunktDeltBosted).isEqualTo(endretUtbetalingAndelRequestDto.avtaletidspunktDeltBosted)
            assertThat(oppdatertEndretUtbetalingAndel.søknadstidspunkt).isEqualTo(endretUtbetalingAndelRequestDto.søknadstidspunkt)
            assertThat(oppdatertEndretUtbetalingAndel.begrunnelse).isEqualTo(endretUtbetalingAndelRequestDto.begrunnelse)
            assertThat(oppdatertEndretUtbetalingAndel.vedtaksbegrunnelser).isEmpty()
            assertThat(oppdatertEndretUtbetalingAndel.erEksplisittAvslagPåSøknad).isEqualTo(endretUtbetalingAndelRequestDto.erEksplisittAvslagPåSøknad)
        }
    }
}
