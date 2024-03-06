package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EndringIEndretUtbetalingAndelUtilTest {
    val jan22 = YearMonth.of(2022, 1)
    val aug22 = YearMonth.of(2022, 8)
    val sep22 = YearMonth.of(2022, 9)
    val des22 = YearMonth.of(2022, 12)

    @Test
    fun `Endring i endret utbetaling andel - skal ha endret periode hvis årsak er endret`() {
        val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                person = barn,
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ETTERBETALING_3MND,
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(årsak = Årsak.ALLEREDE_UTBETALT)

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
            ).tilPerioder().filter { it.verdi == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
        assertEquals(aug22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)
    }


    @Test
    fun `Endring i endret utbetaling andel - skal ikke ha noen endrede perioder hvis kun prosent er endret`() {
        val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                person = barn,
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.DELT_BOSTED,
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(prosent = BigDecimal(100))

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
            ).tilPerioder().filter { it.verdi == true }

        assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i endret utbetaling andel - skal returnere endret periode hvis et av to barn har endring på årsak`() {
        val barn1 = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
        val barn2 = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)

        val forrigeEndretAndelBarn1 =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                person = barn1,
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.DELT_BOSTED,
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val forrigeEndretAndelBarn2 =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                person = barn2,
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.ETTERBETALING_3MND,
            )

        val perioderMedEndring =
            listOf(barn1, barn2).map {
                EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                    forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.person == it },
                    nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT)).filter { endretAndel -> endretAndel.person == it },
                )
            }.flatMap { it.tilPerioder() }.filter { it.verdi == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
        assertEquals(aug22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal noen endrede perioder hvis eneste endring er at perioden blir lenger`() {
        val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
        val forrigeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                person = barn,
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.DELT_BOSTED,
                avtaletidspunktDeltBosted = jan22.førsteDagIInneværendeMåned(),
            )

        val nåværendeEndretAndel = forrigeEndretAndel.copy(tom = des22)

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
            ).tilPerioder().filter { it.verdi == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(sep22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
        assertEquals(des22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)
    }

    @Test
    fun `Endring i endret utbetaling andel - skal ha endrede perioder hvis endringsperiode oppstår i nåværende behandling`() {
        val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
        val nåværendeEndretAndel =
            lagEndretUtbetalingAndel(
                behandlingId = 0,
                person = barn,
                prosent = BigDecimal.ZERO,
                periodeFom = jan22,
                periodeTom = aug22,
                årsak = Årsak.DELT_BOSTED,
            )

        val perioderMedEndring =
            EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                forrigeEndretAndelerForPerson = emptyList(),
                nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
            ).tilPerioder().filter { it.verdi == true }

        assertEquals(1, perioderMedEndring.size)
        assertEquals(jan22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
        assertEquals(aug22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)
    }
}
