package no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler.slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler.tilAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler.tilAndelerTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AndelTilkjentYtelseMedEndretUtbetalingBehandlerTest {
    @Nested
    inner class OppdaterAndelerMedEndringer {
        @Test
        fun `skal returnere tom liste hvis det ikke finnes noen andeler`() {
            // Arrange
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            // Act
            val oppdaterteAndeler =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = emptyList(),
                    endretUtbetalingAndeler = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler).isEmpty()
        }

        @Test
        fun `skal returnere eksisterende andeler uten endringer hvis det ikke finnes noen endret utbetalinger`() {
            // Arrange
            val barn1 = lagPerson(personType = PersonType.BARN)
            val barn2 = lagPerson(personType = PersonType.BARN)
            val behandling = lagBehandling()

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(5),
                    person = barn1,
                    behandling = behandling,
                    beløp = 7500,
                    sats = 7500,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now(),
                    person = barn2,
                    behandling = behandling,
                    beløp = 8000,
                    sats = 8000,
                )

            // Act
            val oppdaterteAndeler =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = listOf(andel1, andel2),
                    endretUtbetalingAndeler = emptyList(),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(2)

            val førsteAndelMedEndring = oppdaterteAndeler.minBy { it.stønadFom }
            assertThat(førsteAndelMedEndring.andel).isEqualTo(andel1)
            assertThat(førsteAndelMedEndring.endreteUtbetalinger).isEmpty()

            val sisteAndelMedEndring = oppdaterteAndeler.maxBy { it.stønadFom }
            assertThat(sisteAndelMedEndring.andel).isEqualTo(andel2)
            assertThat(sisteAndelMedEndring.endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `skal oppdatere andeler for riktig person med endret utbetaling`() {
            // Arrange
            val barn1 = lagPerson(personType = PersonType.BARN)
            val barn2 = lagPerson(personType = PersonType.BARN)
            val behandling = lagBehandling()

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val fom = YearMonth.now().minusMonths(10)
            val tom = YearMonth.now().minusMonths(5)

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = barn1,
                    behandling = behandling,
                    beløp = 7500,
                    sats = 7500,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = fom,
                    tom = tom,
                    person = barn2,
                    behandling = behandling,
                    beløp = 8000,
                    sats = 8000,
                )

            val endretUtbetalingAndelForBarn1 =
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    person = barn1,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ETTERBETALING_3MND,
                    fom = fom,
                    tom = tom,
                )

            // Act
            val oppdaterteAndeler =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = listOf(andel1, andel2),
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelForBarn1),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(2)

            assertThat(oppdaterteAndeler[0].kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(oppdaterteAndeler[0].prosent).isEqualTo(BigDecimal.ZERO)
            assertThat(oppdaterteAndeler[0].aktør).isEqualTo(barn1.aktør)
            assertThat(oppdaterteAndeler[0].endreteUtbetalinger.size).isEqualTo(1)
            assertThat(oppdaterteAndeler[0].endreteUtbetalinger.single()).isEqualTo(endretUtbetalingAndelForBarn1.endretUtbetalingAndel)

            assertThat(oppdaterteAndeler[1].andel).isEqualTo(andel2)
            assertThat(oppdaterteAndeler[1].endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `skal oppdatere andeler med endring som går på tvers av andeler`() {
            // Arrange
            val behandling = lagBehandling()
            val barn = lagPerson(personType = PersonType.BARN)
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = behandling,
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            val andel1 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(5),
                    person = barn,
                    behandling = behandling,
                    beløp = 1000,
                    sats = 1000,
                )

            val andel2 =
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusMonths(4),
                    tom = YearMonth.now(),
                    person = barn,
                    behandling = behandling,
                    beløp = 1500,
                    sats = 1500,
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = behandling.id,
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    fom = YearMonth.now().minusMonths(7),
                    tom = YearMonth.now().minusMonths(2),
                )

            // Act
            val oppdaterteAndeler =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = listOf(andel1, andel2),
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
                    tilkjentYtelse = tilkjentYtelse,
                )

            // Assert
            assertThat(oppdaterteAndeler.size).isEqualTo(3)

            assertThat(oppdaterteAndeler[0].kalkulertUtbetalingsbeløp).isEqualTo(andel1.kalkulertUtbetalingsbeløp)
            assertThat(oppdaterteAndeler[0].prosent).isEqualTo(andel1.prosent)
            assertThat(oppdaterteAndeler[0].stønadFom).isEqualTo(andel1.stønadFom)
            assertThat(oppdaterteAndeler[0].stønadTom).isEqualTo(endretUtbetalingAndel.fom?.minusMonths(1))
            assertThat(oppdaterteAndeler[0].endreteUtbetalinger).isEmpty()

            assertThat(oppdaterteAndeler[1].kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(oppdaterteAndeler[1].prosent).isEqualTo(endretUtbetalingAndel.prosent)
            assertThat(oppdaterteAndeler[1].stønadFom).isEqualTo(endretUtbetalingAndel.fom)
            assertThat(oppdaterteAndeler[1].stønadTom).isEqualTo(endretUtbetalingAndel.tom)
            assertThat(oppdaterteAndeler[1].endreteUtbetalinger.size).isEqualTo(1)
            assertThat(oppdaterteAndeler[1].endreteUtbetalinger.single()).isEqualTo(endretUtbetalingAndel.endretUtbetalingAndel)

            assertThat(oppdaterteAndeler[2].kalkulertUtbetalingsbeløp).isEqualTo(andel2.kalkulertUtbetalingsbeløp)
            assertThat(oppdaterteAndeler[2].prosent).isEqualTo(andel2.prosent)
            assertThat(oppdaterteAndeler[2].stønadFom).isEqualTo(endretUtbetalingAndel.tom?.plusMonths(1))
            assertThat(oppdaterteAndeler[2].stønadTom).isEqualTo(andel2.stønadTom)
            assertThat(oppdaterteAndeler[2].endreteUtbetalinger).isEmpty()
        }

        @Test
        fun `endret utbetalingsandel skal overstyre andel`() {
            val søker = lagPerson(personType = PersonType.SØKER)
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val fom = YearMonth.of(2018, 1)
            val tom = YearMonth.of(2019, 1)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        fom = fom,
                        tom = tom,
                        person = søker,
                        behandling = behandling,
                        beløp = 1500,
                        sats = 1500,
                    ),
                )

            val endretProsent = BigDecimal.ZERO

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søker,
                    periodeFom = fom,
                    periodeTom = tom,
                    prosent = BigDecimal.ZERO,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            val andelerTilkjentYtelse =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = utbetalingsandeler,
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                    tilkjentYtelse = utbetalingsandeler.first().tilkjentYtelse,
                )

            assertEquals(1, andelerTilkjentYtelse.size)
            assertEquals(endretProsent, andelerTilkjentYtelse.single().prosent)
            assertEquals(1, andelerTilkjentYtelse.single().endreteUtbetalinger.size)
        }

        @Test
        fun `endret utbetalingsandel kobler endrede andeler til riktig endret utbetalingandel`() {
            val søker = lagPerson(personType = PersonType.SØKER)
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val fom1 = YearMonth.of(2018, 1)
            val tom1 = YearMonth.of(2018, 11)

            val fom2 = YearMonth.of(2019, 1)
            val tom2 = YearMonth.of(2019, 11)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = fom1,
                        stønadTom = tom1,
                        aktør = søker.aktør,
                        behandling = behandling,
                    ),
                    lagAndelTilkjentYtelse(
                        stønadFom = fom2,
                        stønadTom = tom2,
                        aktør = søker.aktør,
                        behandling = behandling,
                    ),
                )

            val endretProsent = BigDecimal.ZERO

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søker,
                    periodeFom = fom1,
                    periodeTom = tom2,
                    prosent = BigDecimal.ZERO,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse1 =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            val endretUtbetalingAndel2 =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søker,
                    periodeFom = tom2.nesteMåned(),
                    prosent = endretProsent,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse2 =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel2, utbetalingsandeler)

            val andelerTilkjentYtelse =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = utbetalingsandeler,
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse1, endretUtbetalingAndelMedAndelerTilkjentYtelse2),
                    tilkjentYtelse = utbetalingsandeler.first().tilkjentYtelse,
                )

            assertEquals(2, andelerTilkjentYtelse.size)
            andelerTilkjentYtelse.forEach { assertEquals(endretProsent, it.prosent) }
            andelerTilkjentYtelse.forEach { assertEquals(1, it.endreteUtbetalinger.size) }
            andelerTilkjentYtelse.forEach {
                assertEquals(
                    endretUtbetalingAndel.id,
                    it.endreteUtbetalinger.single().id,
                )
            }
        }

        @Test
        fun `skal ikke overstyre andel ved allerede utbetalt med prosent høyere enn 0`() {
            val søker = lagPerson(personType = PersonType.SØKER)
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val fom = YearMonth.of(2018, 1)
            val tom = YearMonth.of(2019, 1)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = fom,
                        stønadTom = tom,
                        aktør = søker.aktør,
                        behandling = behandling,
                        prosent = BigDecimal(75),
                        kalkulertUtbetalingsbeløp = 6000,
                    ),
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søker,
                    periodeFom = fom,
                    periodeTom = tom,
                    prosent = BigDecimal(100),
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            val andelerTilkjentYtelse =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = utbetalingsandeler,
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                    tilkjentYtelse = utbetalingsandeler.first().tilkjentYtelse,
                )
            val andelTilkjentYtelse = andelerTilkjentYtelse.single()

            assertEquals(1, andelerTilkjentYtelse.size)
            assertEquals(BigDecimal(75), andelTilkjentYtelse.prosent)
            assertEquals(6000, andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
            assertEquals(1, andelTilkjentYtelse.endreteUtbetalinger.size)
        }

        @Test
        fun `skal overstyre andel ved allerede utbetalt med prosent høyere lik 0`() {
            val søker = lagPerson(personType = PersonType.SØKER)
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val fom = YearMonth.of(2018, 1)
            val tom = YearMonth.of(2019, 1)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = fom,
                        stønadTom = tom,
                        aktør = søker.aktør,
                        behandling = behandling,
                        prosent = BigDecimal(75),
                        kalkulertUtbetalingsbeløp = 6000,
                    ),
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søker,
                    periodeFom = fom,
                    periodeTom = tom,
                    prosent = BigDecimal(0),
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            val andelerTilkjentYtelse =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = utbetalingsandeler,
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                    tilkjentYtelse = utbetalingsandeler.first().tilkjentYtelse,
                )
            val andelTilkjentYtelse = andelerTilkjentYtelse.single()

            assertEquals(1, andelerTilkjentYtelse.size)
            assertEquals(BigDecimal(0), andelTilkjentYtelse.prosent)
            assertEquals(0, andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
            assertEquals(1, andelTilkjentYtelse.endreteUtbetalinger.size)
        }

        @ParameterizedTest
        @EnumSource(
            value = YtelseType::class,
            names = ["ORDINÆR_KONTANTSTØTTE"],
            mode = EnumSource.Mode.EXCLUDE
        )
        fun `skal kaste feil hvis man prøver å oppdatere andeler som ikke er ordinær kontantstøtte`(ytelseType: YtelseType) {
            val søker = lagPerson(personType = PersonType.SØKER)
            val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
            val fom = YearMonth.of(2018, 1)
            val tom = YearMonth.of(2019, 1)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = fom,
                        stønadTom = tom,
                        aktør = søker.aktør,
                        behandling = behandling,
                        prosent = BigDecimal(75),
                        kalkulertUtbetalingsbeløp = 6000,
                        ytelseType = ytelseType,
                    ),
                )

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søker,
                    periodeFom = fom,
                    periodeTom = tom,
                    prosent = BigDecimal(0),
                    årsak = Årsak.ALLEREDE_UTBETALT,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            assertThrows<Feil> {
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
                    andelTilkjentYtelserUtenEndringer = utbetalingsandeler,
                    endretUtbetalingAndeler = listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                    tilkjentYtelse = utbetalingsandeler.first().tilkjentYtelse,
                )
            }
        }
    }

    @Nested
    inner class SlåSammenEtterfølgendeAndelerTest {
        @Test
        fun `skal ikke slå sammen etterfølgende 0kr-andeler hvis de ikke skyldes samme endret utbetaling andel`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)

            val periode1 =
                Periode(
                    fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel =
                                        lagEndretUtbetalingAndel(
                                            person = barn,
                                            prosent = BigDecimal.ZERO,
                                            årsak = Årsak.ETTERBETALING_3MND,
                                        ),
                                ),
                        ),
                )

            val periode2 =
                Periode(
                    fom = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel =
                                        lagEndretUtbetalingAndel(
                                            person = barn,
                                            prosent = BigDecimal.ZERO,
                                            årsak = Årsak.ALLEREDE_UTBETALT,
                                        ),
                                ),
                        ),
                )

            val perioderMedAndeler = listOf(periode1, periode2)

            // Act
            val perioderEtterSammenslåing =
                perioderMedAndeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
            assertThat(perioderEtterSammenslåing[0]).isEqualTo(periode1)
            assertThat(perioderEtterSammenslåing[1]).isEqualTo(periode2)
        }

        @Test
        fun `skal ikke slå sammen 0kr-andeler som har tom periode mellom seg`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(person = barn, prosent = BigDecimal.ZERO, årsak = Årsak.ALLEREDE_UTBETALT)

            val periode1 =
                Periode(
                    fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel = endretUtbetalingAndel,
                                ),
                        ),
                )

            val periode2 =
                Periode(
                    fom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel = endretUtbetalingAndel,
                                ),
                        ),
                )
            val perioderMedAndeler = listOf(periode1, periode2)

            // Act
            val perioderEtterSammenslåing =
                perioderMedAndeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
            assertThat(perioderEtterSammenslåing[0]).isEqualTo(periode1)
            assertThat(perioderEtterSammenslåing[1]).isEqualTo(periode2)
        }

        @Test
        fun `skal ikke slå sammen etterfølgende andeler med 100 prosent utbetaling av ulik sats`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)

            val periode1 =
                Periode(
                    fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 1054,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal(100),
                            endretUtbetalingAndel = null,
                        ),
                )

            val periode2 =
                Periode(
                    fom = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 1766,
                            sats = 1766,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal(100),
                            endretUtbetalingAndel = null,
                        ),
                )
            val perioderMedAndeler = listOf(periode1, periode2)

            // Act
            val perioderEtterSammenslåing =
                perioderMedAndeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(2)
            assertThat(perioderEtterSammenslåing[0]).isEqualTo(periode1)
            assertThat(perioderEtterSammenslåing[1]).isEqualTo(periode2)
        }

        @Test
        fun `skal slå sammen etterfølgende 0kr-andeler som skyldes samme endret andel, men er splittet pga satsendring`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)
            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    person = barn,
                    prosent = BigDecimal.ZERO,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    periodeFom = YearMonth.now().minusMonths(9),
                    periodeTom = YearMonth.now(),
                )

            val periode1 =
                Periode(
                    fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel = endretUtbetalingAndel,
                                ),
                        ),
                )

            val periode2 =
                Periode(
                    fom = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                    tom = LocalDate.now().sisteDagIMåned(),
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1766,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel = endretUtbetalingAndel,
                                ),
                        ),
                )

            val perioderMedAndeler = listOf(periode1, periode2)

            // Act
            val perioderEtterSammenslåing =
                perioderMedAndeler.tilTidslinje().slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel().tilPerioderIkkeNull()

            // Assert
            assertThat(perioderEtterSammenslåing.size).isEqualTo(1)
            val periode = perioderEtterSammenslåing.single()
            assertThat(periode.verdi).isEqualTo(periode1.verdi)
            assertThat(periode.fom).isEqualTo(periode1.fom)
            assertThat(periode.tom).isEqualTo(periode2.tom)
        }
    }

    @Nested
    inner class FraTidslinjeTilAndelerTest {
        @Test
        fun `skal lage AndelTilkjentYtelseMedEndreteUtbetalinger uten endring hvis perioden ikke er knyttet til en endret utbetaling`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)
            val fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned()
            val tom = LocalDate.now().minusMonths(5).sisteDagIMåned()
            val periode =
                Periode(
                    fom = fom,
                    tom = tom,
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 1054,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal(100),
                            endretUtbetalingAndel = null,
                        ),
                )
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            // Act
            val andel = periode.tilAndelTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)

            // Assert
            assertThat(andel.endreteUtbetalinger.size).isEqualTo(0)
            assertThat(andel.andel.tilkjentYtelse).isEqualTo(tilkjentYtelse)
            assertThat(andel.andel.aktør).isEqualTo(barn.aktør)
            assertThat(andel.andel.type).isEqualTo(YtelseType.ORDINÆR_KONTANTSTØTTE)
            assertThat(andel.andel.kalkulertUtbetalingsbeløp).isEqualTo(1054)
            assertThat(andel.andel.nasjonaltPeriodebeløp).isEqualTo(1054)
            assertThat(andel.andel.differanseberegnetPeriodebeløp).isEqualTo(null)
            assertThat(andel.andel.sats).isEqualTo(1054)
            assertThat(andel.andel.prosent).isEqualTo(BigDecimal(100))
            assertThat(andel.andel.stønadFom).isEqualTo(fom.toYearMonth())
            assertThat(andel.andel.stønadTom).isEqualTo(tom.toYearMonth())
        }

        @Test
        fun `skal lage AndelTilkjentYtelseMedEndreteUtbetalinger med endring hvis perioden er knyttet til en endret utbetaling`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)
            val fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned()
            val tom = LocalDate.now().minusMonths(5).sisteDagIMåned()
            val periode =
                Periode(
                    fom = fom,
                    tom = tom,
                    verdi =
                        AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                            aktør = barn.aktør,
                            beløp = 0,
                            sats = 1054,
                            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                            prosent = BigDecimal.ZERO,
                            endretUtbetalingAndel =
                                EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                    andeler = emptyList(),
                                    endretUtbetalingAndel =
                                        lagEndretUtbetalingAndel(
                                            person = barn,
                                            prosent = BigDecimal.ZERO,
                                            årsak = Årsak.ETTERBETALING_3MND,
                                        ),
                                ),
                        ),
                )
            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            // Act
            val andel = periode.tilAndelTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)

            // Assert
            assertThat(andel.endreteUtbetalinger.size).isEqualTo(1)
            assertThat(andel.endreteUtbetalinger.single().prosent).isEqualTo(BigDecimal.ZERO)
            assertThat(andel.endreteUtbetalinger.single().årsak).isEqualTo(Årsak.ETTERBETALING_3MND)
            assertThat(andel.endreteUtbetalinger.single().person).isEqualTo(barn)

            assertThat(andel.andel.tilkjentYtelse).isEqualTo(tilkjentYtelse)
            assertThat(andel.andel.aktør).isEqualTo(barn.aktør)
            assertThat(andel.andel.type).isEqualTo(YtelseType.ORDINÆR_KONTANTSTØTTE)
            assertThat(andel.andel.kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(andel.andel.nasjonaltPeriodebeløp).isEqualTo(0)
            assertThat(andel.andel.differanseberegnetPeriodebeløp).isEqualTo(null)
            assertThat(andel.andel.sats).isEqualTo(1054)
            assertThat(andel.andel.prosent).isEqualTo(BigDecimal.ZERO)
            assertThat(andel.andel.stønadFom).isEqualTo(fom.toYearMonth())
            assertThat(andel.andel.stønadTom).isEqualTo(tom.toYearMonth())
        }

        @Test
        fun `skal lage andeler kun for perioder med verdi`() {
            // Arrange
            val barn = lagPerson(personType = PersonType.BARN)
            val perioder =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusMonths(9).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 0,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                                prosent = BigDecimal.ZERO,
                                endretUtbetalingAndel =
                                    EndretUtbetalingAndelMedAndelerTilkjentYtelse(
                                        andeler = emptyList(),
                                        endretUtbetalingAndel =
                                            lagEndretUtbetalingAndel(
                                                person = barn,
                                                prosent = BigDecimal.ZERO,
                                                årsak = Årsak.ETTERBETALING_3MND,
                                            ),
                                    ),
                            ),
                    ),
                    Periode(
                        fom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                        tom = LocalDate.now().sisteDagIMåned(),
                        verdi =
                            AndelTilkjentYtelseMedEndretUtbetalingBehandler.AndelMedEndretUtbetalingForTidslinje(
                                aktør = barn.aktør,
                                beløp = 1054,
                                sats = 1054,
                                ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                                prosent = BigDecimal(100),
                                endretUtbetalingAndel = null,
                            ),
                    ),
                )

            val tilkjentYtelse =
                TilkjentYtelse(
                    behandling = lagBehandling(),
                    endretDato = LocalDate.now(),
                    opprettetDato = LocalDate.now().minusMonths(9),
                )

            // Act
            val tidslinje = perioder.tilTidslinje()
            // Dobbeltsjekker at det blir laget en null-periode mellom de to periodene med verdi
            assertThat(tidslinje.tilPerioder().size).isEqualTo(3)

            val andeler = tidslinje.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)

            // Assert
            assertThat(andeler.size).isEqualTo(2)
            assertThat(andeler[0].kalkulertUtbetalingsbeløp).isEqualTo(0)
            assertThat(andeler[1].kalkulertUtbetalingsbeløp).isEqualTo(1054)
        }
    }
}
