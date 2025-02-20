package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EndringIUtbetalingUtilTest {
    private val jan22 = YearMonth.of(2022, 1)
    private val aug22 = YearMonth.of(2022, 8)
    private val sep22 = YearMonth.of(2022, 9)
    private val des22 = YearMonth.of(2022, 12)

    @Nested
    inner class UtledEndringstidspunktForUtbetalingsbeløpTest {
        @Test
        fun `Skal returnere riktig endringstidspunkt når ny andel med beløp større enn 0 er lagt til`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn1Aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn2Aktør,
                    ),
                )
            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = des22,
                        beløp = 1054,
                        aktør = barn1Aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn2Aktør,
                    ),
                )

            val endringstidspunkt =
                EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                    nåværendeAndeler = nåværendeAndeler,
                    forrigeAndeler = forrigeAndeler,
                )

            assertThat(sep22).isEqualTo(endringstidspunkt)
        }

        @Test
        fun `Endringstidspunktet skal ikke settes hvis andelene er helt like forrige behandling og nå`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val andeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn1Aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn2Aktør,
                    ),
                )

            val endringstidspunkt =
                EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                    nåværendeAndeler = andeler,
                    forrigeAndeler = andeler,
                )

            Assertions.assertNull(endringstidspunkt)
        }

        @Test
        fun `Riktig endringstidspunkt skal settes dersom det er fjernet andel som har beløp større enn 0`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val andelBarn1 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                )
            val andelBarn2 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                )

            val endringstidspunkt =
                EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                    nåværendeAndeler = listOf(andelBarn2),
                    forrigeAndeler = listOf(andelBarn2, andelBarn1),
                )

            assertThat(jan22).isEqualTo(endringstidspunkt)
        }

        @Test
        fun `Endringstidspunkt skal ikke settes dersom andel med 0 i beløp er fjernet`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val andelBarn1 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 0,
                    aktør = barn1Aktør,
                )
            val andelBarn2 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                )

            val endringstidspunkt =
                EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                    nåværendeAndeler = listOf(andelBarn2),
                    forrigeAndeler = listOf(andelBarn2, andelBarn1),
                )

            Assertions.assertNull(endringstidspunkt)
        }
    }

    @Nested
    inner class LagEndringIUtbetalingTidslinjeTest {
        @Test
        fun `Skal returnere periode med endring når ny andel med beløp større enn 0 er lagt til`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val forrigeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn1Aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn2Aktør,
                    ),
                )
            val nåværendeAndeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = des22,
                        beløp = 1054,
                        aktør = barn1Aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn2Aktør,
                    ),
                )

            val perioderMedEndring =
                EndringIUtbetalingUtil
                    .lagEndringIUtbetalingTidslinje(
                        nåværendeAndeler = nåværendeAndeler,
                        forrigeAndeler = forrigeAndeler,
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(sep22).isEqualTo(perioderMedEndring.single().fom?.tilYearMonth())
            assertThat(des22).isEqualTo(perioderMedEndring.single().tom?.tilYearMonth())
        }

        @Test
        fun `Skal ikke gi noen perioder med endring hvis andelene er helt like forrige behandling og nå`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val andeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn1Aktør,
                    ),
                    lagAndelTilkjentYtelse(
                        fom = jan22,
                        tom = aug22,
                        beløp = 1054,
                        aktør = barn2Aktør,
                    ),
                )

            val perioderMedEndring =
                EndringIUtbetalingUtil
                    .lagEndringIUtbetalingTidslinje(
                        nåværendeAndeler = andeler,
                        forrigeAndeler = andeler,
                    ).tilPerioder()
                    .filter { it.verdi == true }

            Assertions.assertTrue(perioderMedEndring.isEmpty())
        }

        @Test
        fun `Skal returnere periode med endring hvis andel med beløp større enn 0 er fjernet`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val andelBarn1 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Aktør,
                )
            val andelBarn2 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                )

            val perioderMedEndring =
                EndringIUtbetalingUtil
                    .lagEndringIUtbetalingTidslinje(
                        nåværendeAndeler = listOf(andelBarn2),
                        forrigeAndeler = listOf(andelBarn2, andelBarn1),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(jan22).isEqualTo(perioderMedEndring.single().fom?.tilYearMonth())
            assertThat(aug22).isEqualTo(perioderMedEndring.single().tom?.tilYearMonth())
        }

        @Test
        fun `Skal ikke returnere periode med endring hvis andel med 0 i beløp er fjernet`() {
            val barn1Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør
            val barn2Aktør = lagPerson(aktør = randomAktør(), personType = PersonType.BARN).aktør

            val andelBarn1 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 0,
                    aktør = barn1Aktør,
                )
            val andelBarn2 =
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn2Aktør,
                )

            val perioderMedEndring =
                EndringIUtbetalingUtil
                    .lagEndringIUtbetalingTidslinje(
                        nåværendeAndeler = listOf(andelBarn2),
                        forrigeAndeler = listOf(andelBarn2, andelBarn1),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            Assertions.assertTrue(perioderMedEndring.isEmpty())
        }
    }
}
