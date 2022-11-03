package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValidator.validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

internal class TilkjentYtelseValidatorTest {

    val søker = randomAktør()
    val barn = randomAktør()
    val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søker.aktivFødselsnummer(),
        barnasIdenter = listOf(barn.aktivFødselsnummer()),
        søkerAktør = søker,
        barnAktør = listOf(barn)
    )
    val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling).also {
        it.stønadFom = YearMonth.now().minusMonths(11)
        it.stønadTom = YearMonth.now()
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når utbetalingsperiode er mer enn 11 måneder`() {
        val tilkjentYtelse = lagInitieltTilkjentYtelse(behandling).also {
            it.stønadFom = YearMonth.now().minusYears(1)
            it.stønadTom = YearMonth.now()
        }
        val exception = assertThrows<FunksjonellFeil> {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }
        val feilmelding = "Kontantstøtte kan maks utbetales for 11 måneder. Du er i ferd med å utbetale mer enn dette. " +
            "Kontroller datoene på vilkårene eller ta kontakt med team familie"

        assertEquals(feilmelding, exception.frontendFeilmelding)
        assertEquals(feilmelding, exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når søkers andel ikke er tom`() {
        val andelTilkjentYtelseForSøker = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = søker
        )
        val andelTilkjentYtelseForBarn = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(listOf(andelTilkjentYtelseForSøker, andelTilkjentYtelseForBarn))

        val exception = assertThrows<Feil> {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }

        assertEquals("Feil i beregning. Søkers andeler må være tom", exception.message)
    }

    @Test
    fun `validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp skal kaste feil når barn har andel med beløp som er større en maks beløp`() {
        val andelTilkjentYtelseForBarn = lagAndelTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            behandling = behandling,
            aktør = barn,
            stønadFom = YearMonth.now().minusMonths(11),
            stønadTom = YearMonth.now().minusMonths(6),
            sats = 8000
        )
        tilkjentYtelse.andelerTilkjentYtelse.add(andelTilkjentYtelseForBarn)

        val andeler = tilkjentYtelse.andelerTilkjentYtelse

        val exception = assertThrows<FunksjonellFeil> {
            validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(tilkjentYtelse, personopplysningGrunnlag)
        }

        assertEquals(
            "Validering av andeler for BARN i perioden (${andeler.first().stønadFom} - " +
                "${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = ${maksBeløp()}, faktiske totalbeløp = 8000.",
            exception.message
        )
        assertEquals(
            "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
            exception.frontendFeilmelding
        )
    }
}
