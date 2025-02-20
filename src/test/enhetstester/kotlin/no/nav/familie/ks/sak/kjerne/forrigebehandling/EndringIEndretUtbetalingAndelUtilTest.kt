package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class EndringIEndretUtbetalingAndelUtilTest {
    private val jan22 = YearMonth.of(2022, 1)
    private val aug22 = YearMonth.of(2022, 8)
    private val sep22 = YearMonth.of(2022, 9)
    private val des22 = YearMonth.of(2022, 12)

    @Nested
    inner class UtledEndringstidspunktForEndretUtbetalingAndelTest {
        @Test
        fun `Skal utlede riktig endringstidspunkt når årsak er endret`() {
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

            val endringstidspunkt =
                EndringIEndretUtbetalingAndelUtil.utledEndringstidspunktForEndretUtbetalingAndel(
                    forrigeEndretAndeler = listOf(forrigeEndretAndel),
                    nåværendeEndretAndeler = listOf(nåværendeEndretAndel),
                )

            assertThat(jan22).isEqualTo(endringstidspunkt)
        }

        @Test
        fun `Endring av prosent skal ikke trigge at endringstidspunktet blir endret`() {
            val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
            val forrigeEndretAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = 0,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    periodeFom = jan22,
                    periodeTom = aug22,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val nåværendeEndretAndel = forrigeEndretAndel.copy(prosent = BigDecimal(100))

            val endringstidspunkt =
                EndringIEndretUtbetalingAndelUtil.utledEndringstidspunktForEndretUtbetalingAndel(
                    forrigeEndretAndeler = listOf(forrigeEndretAndel),
                    nåværendeEndretAndeler = listOf(nåværendeEndretAndel),
                )

            assertNull(endringstidspunkt)
        }

        @Test
        fun `Skal utlede riktig endringstidspunkt når det har vært endring på årsak på et av to barn`() {
            val barn1 = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
            val barn2 = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)

            val forrigeEndretAndelBarn1 =
                lagEndretUtbetalingAndel(
                    behandlingId = 0,
                    person = barn1,
                    prosent = BigDecimal.ZERO,
                    periodeFom = jan22,
                    periodeTom = aug22,
                    årsak = Årsak.ALLEREDE_UTBETALT,
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

            val endringstidspunkt =
                EndringIEndretUtbetalingAndelUtil.utledEndringstidspunktForEndretUtbetalingAndel(
                    forrigeEndretAndeler = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2),
                    nåværendeEndretAndeler =
                        listOf(
                            forrigeEndretAndelBarn1,
                            forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT),
                        ),
                )
            assertThat(jan22).isEqualTo(endringstidspunkt)
        }
    }

    @Nested
    inner class LagEndringIEndretUbetalingAndelPerPersonTidslinje {
        @Test
        fun `Skal ha endret periode hvis årsak er endret`() {
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
                EndringIEndretUtbetalingAndelUtil
                    .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                        nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(jan22.førsteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().fom)
            assertThat(aug22.sisteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().tom)
        }

        @Test
        fun `Skal ikke ha noen endrede perioder hvis kun prosent er endret`() {
            val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
            val forrigeEndretAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = 0,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    periodeFom = jan22,
                    periodeTom = aug22,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val nåværendeEndretAndel = forrigeEndretAndel.copy(prosent = BigDecimal(100))

            val perioderMedEndring =
                EndringIEndretUtbetalingAndelUtil
                    .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                        nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertTrue(perioderMedEndring.isEmpty())
        }

        @Test
        fun `Skal returnere endret periode hvis et av to barn har endring på årsak`() {
            val barn1 = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
            val barn2 = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)

            val forrigeEndretAndelBarn1 =
                lagEndretUtbetalingAndel(
                    behandlingId = 0,
                    person = barn1,
                    prosent = BigDecimal.ZERO,
                    periodeFom = jan22,
                    periodeTom = aug22,
                    årsak = Årsak.ALLEREDE_UTBETALT,
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
                listOf(barn1, barn2)
                    .map {
                        EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                            forrigeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2).filter { endretAndel -> endretAndel.person == it },
                            nåværendeEndretAndelerForPerson = listOf(forrigeEndretAndelBarn1, forrigeEndretAndelBarn2.copy(årsak = Årsak.ALLEREDE_UTBETALT)).filter { endretAndel -> endretAndel.person == it },
                        )
                    }.flatMap { it.tilPerioder() }
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(jan22.førsteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().fom)
            assertThat(aug22.sisteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().tom)
        }

        @Test
        fun `Skal noen endrede perioder hvis eneste endring er at perioden blir lenger`() {
            val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
            val forrigeEndretAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = 0,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    periodeFom = jan22,
                    periodeTom = aug22,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val nåværendeEndretAndel = forrigeEndretAndel.copy(tom = des22)

            val perioderMedEndring =
                EndringIEndretUtbetalingAndelUtil
                    .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = listOf(forrigeEndretAndel),
                        nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(sep22.førsteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().fom)
            assertThat(des22.sisteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().tom)
        }

        @Test
        fun `Skal ha endrede perioder hvis endringsperiode oppstår i nåværende behandling`() {
            val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
            val nåværendeEndretAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = 0,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    periodeFom = jan22,
                    periodeTom = aug22,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val perioderMedEndring =
                EndringIEndretUtbetalingAndelUtil
                    .lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        forrigeEndretAndelerForPerson = emptyList(),
                        nåværendeEndretAndelerForPerson = listOf(nåværendeEndretAndel),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(jan22.førsteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().fom)
            assertThat(aug22.sisteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().tom)
        }
    }
}
