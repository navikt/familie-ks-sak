package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EndringIUtbetalingUtilTest {
    val jan22 = YearMonth.of(2022, 1)
    val aug22 = YearMonth.of(2022, 8)
    val sep22 = YearMonth.of(2022, 9)
    val des22 = YearMonth.of(2022, 12)

    @Test
    fun `Endring i beløp - Skal returnere periode med endring når ny andel med beløp større enn 0 er lagt til`() {
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

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(sep22, perioderMedEndring.single().fom?.tilYearMonth())
        Assertions.assertEquals(des22, perioderMedEndring.single().tom?.tilYearMonth())

        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = nåværendeAndeler,
                forrigeAndeler = forrigeAndeler,
            )

        Assertions.assertEquals(sep22, endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal ikke gi noen perioder med endring hvis andelene er helt like forrige behandling og nå`() {
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

        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = andeler,
                forrigeAndeler = andeler,
            )

        Assertions.assertNull(endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal returnere periode med endring hvis andel med beløp større enn 0 er fjernet`() {
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

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22, perioderMedEndring.single().fom?.tilYearMonth())
        Assertions.assertEquals(aug22, perioderMedEndring.single().tom?.tilYearMonth())

        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = listOf(andelBarn2),
                forrigeAndeler = listOf(andelBarn2, andelBarn1),
            )

        Assertions.assertEquals(jan22, endringstidspunkt)
    }

    @Test
    fun `Endring i beløp - Skal ikke returnere periode med endring hvis andel med 0 i beløp er fjernet`() {
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

        val endringstidspunkt =
            EndringIUtbetalingUtil.utledEndringstidspunktForUtbetalingsbeløp(
                nåværendeAndeler = listOf(andelBarn2),
                forrigeAndeler = listOf(andelBarn2, andelBarn1),
            )

        Assertions.assertNull(endringstidspunkt)
    }
}
