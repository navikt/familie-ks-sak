package no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling

import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class AndelTilkjentYtelseMedEndretUtbetalingBehandlerTest {
    private val søker = randomAktør()
    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val barn1 = randomAktør("01012112345")
    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            barnasIdenter = listOf(barn1.aktivFødselsnummer()),
            søkerAktør = søker,
            barnAktør = listOf(barn1),
        )
    private val søkerPerson = lagPerson(personopplysningGrunnlag, søker, PersonType.SØKER)

    @Nested
    inner class OppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler {
        @Test
        fun `oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler - endret utbetalingsandel skal overstyre andel`() {
            val fom = YearMonth.of(2018, 1)
            val tom = YearMonth.of(2019, 1)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = fom,
                        stønadTom = tom,
                        aktør = søker,
                        behandling = behandling,
                    ),
                )

            val endretProsent = BigDecimal.ZERO

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    periodeFom = fom,
                    periodeTom = tom,
                    prosent = BigDecimal.ZERO,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            val andelerTilkjentYtelse =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndelerGammel(
                    utbetalingsandeler,
                    listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
                )

            assertEquals(1, andelerTilkjentYtelse.size)
            assertEquals(endretProsent, andelerTilkjentYtelse.single().prosent)
            assertEquals(1, andelerTilkjentYtelse.single().endreteUtbetalinger.size)
        }

        @Test
        fun `oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler - endret utbetalingsandel koble endrede andeler til riktig endret utbetalingandel`() {
            val fom1 = YearMonth.of(2018, 1)
            val tom1 = YearMonth.of(2018, 11)

            val fom2 = YearMonth.of(2019, 1)
            val tom2 = YearMonth.of(2019, 11)

            val utbetalingsandeler =
                listOf(
                    lagAndelTilkjentYtelse(
                        stønadFom = fom1,
                        stønadTom = tom1,
                        aktør = søker,
                        behandling = behandling,
                    ),
                    lagAndelTilkjentYtelse(
                        stønadFom = fom2,
                        stønadTom = tom2,
                        aktør = søker,
                        behandling = behandling,
                    ),
                )

            val endretProsent = BigDecimal.ZERO

            val endretUtbetalingAndel =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    periodeFom = fom1,
                    periodeTom = tom2,
                    prosent = BigDecimal.ZERO,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse1 =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

            val endretUtbetalingAndel2 =
                lagEndretUtbetalingAndel(
                    behandlingId = behandling.id,
                    person = søkerPerson,
                    periodeFom = tom2.nesteMåned(),
                    prosent = endretProsent,
                )

            val endretUtbetalingAndelMedAndelerTilkjentYtelse2 =
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel2, utbetalingsandeler)

            val andelerTilkjentYtelse =
                AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndelerGammel(
                    utbetalingsandeler,
                    listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse1, endretUtbetalingAndelMedAndelerTilkjentYtelse2),
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
    }

    @Test
    fun `oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler - endret utbetalingsandel skal ikke overstyre andel ved allerede utbetalt med prosent høyere enn 0`() {
        val fom = YearMonth.of(2018, 1)
        val tom = YearMonth.of(2019, 1)

        val utbetalingsandeler =
            listOf(
                lagAndelTilkjentYtelse(
                    stønadFom = fom,
                    stønadTom = tom,
                    aktør = søker,
                    behandling = behandling,
                    prosent = BigDecimal(75),
                    kalkulertUtbetalingsbeløp = 6000,
                ),
            )

        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = søkerPerson,
                periodeFom = fom,
                periodeTom = tom,
                prosent = BigDecimal(100),
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val endretUtbetalingAndelMedAndelerTilkjentYtelse =
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

        val andelerTilkjentYtelse =
            AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndelerGammel(
                utbetalingsandeler,
                listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
            )
        val andelTilkjentYtelse = andelerTilkjentYtelse.single()

        assertEquals(1, andelerTilkjentYtelse.size)
        assertEquals(BigDecimal(75), andelTilkjentYtelse.prosent)
        assertEquals(6000, andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
        assertEquals(1, andelTilkjentYtelse.endreteUtbetalinger.size)
    }

    @Test
    fun `oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler - endret utbetalingsandel skal overstyre andel ved allerede utbetalt med prosent høyere lik 0`() {
        val fom = YearMonth.of(2018, 1)
        val tom = YearMonth.of(2019, 1)

        val utbetalingsandeler =
            listOf(
                lagAndelTilkjentYtelse(
                    stønadFom = fom,
                    stønadTom = tom,
                    aktør = søker,
                    behandling = behandling,
                    prosent = BigDecimal(75),
                    kalkulertUtbetalingsbeløp = 6000,
                ),
            )

        val endretUtbetalingAndel =
            lagEndretUtbetalingAndel(
                behandlingId = behandling.id,
                person = søkerPerson,
                periodeFom = fom,
                periodeTom = tom,
                prosent = BigDecimal(0),
                årsak = Årsak.ALLEREDE_UTBETALT,
            )

        val endretUtbetalingAndelMedAndelerTilkjentYtelse =
            EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, utbetalingsandeler)

        val andelerTilkjentYtelse =
            AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndelerGammel(
                utbetalingsandeler,
                listOf(endretUtbetalingAndelMedAndelerTilkjentYtelse),
            )
        val andelTilkjentYtelse = andelerTilkjentYtelse.single()

        assertEquals(1, andelerTilkjentYtelse.size)
        assertEquals(BigDecimal(0), andelTilkjentYtelse.prosent)
        assertEquals(0, andelTilkjentYtelse.kalkulertUtbetalingsbeløp)
        assertEquals(1, andelTilkjentYtelse.endreteUtbetalinger.size)
    }
}
