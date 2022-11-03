package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.common.exception.UtbetalingsikkerhetFeil
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.Period

object TilkjentYtelseValidator {

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna

        val diff = Period.between(tilkjentYtelse.stønadFom?.toLocalDate(), tilkjentYtelse.stønadTom?.toLocalDate())
        if (diff.toTotalMonths() > 11) {
            val feilmelding = "Kontantstøtte kan maks utbetales for 11 måneder. Du er i ferd med å utbetale mer enn dette. " +
                "Kontroller datoene på vilkårene eller ta kontakt med team familie"
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        tidslinjeMedAndeler.tilPerioder().forEach {
            if (hentSøkersAndeler(it.verdi!!.toList(), søker).isNotEmpty()) {
                throw Feil("Feil i beregning. Søkers andeler må være tom")
            }
            val barnasAndeler = hentBarnasAndeler(it.verdi.toList(), barna)
            barnasAndeler.forEach { (_, andeler) -> validerAtBeløpForPartStemmerMedSatser(andeler) }
        }
    }

    private fun hentSøkersAndeler(
        andeler: List<AndelTilkjentYtelse>,
        søker: Person
    ) = andeler.filter { it.aktør == søker.aktør }

    private fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
        barn to andeler.filter { it.aktør == barn.aktør }
    }

    private fun validerAtBeløpForPartStemmerMedSatser(andeler: List<AndelTilkjentYtelse>) {
        val maksAntallAndeler = 1
        val maksTotalBeløp = maksBeløp()

        val feilMelding = "Validering av andeler for BARN i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet"
        val frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX"

        val totalbeløp = andeler.map { it.kalkulertUtbetalingsbeløp }.fold(0) { sum, beløp -> sum + beløp }

        when {
            andeler.size > maksAntallAndeler -> {
                throw UtbetalingsikkerhetFeil(
                    melding = "$feilMelding: Tillatte andeler = $maksAntallAndeler, faktiske andeler = ${andeler.size}.",
                    frontendFeilmelding = frontendFeilmelding
                )
            }
            totalbeløp > maksTotalBeløp -> {
                throw UtbetalingsikkerhetFeil(
                    melding = "$feilMelding: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
                    frontendFeilmelding = frontendFeilmelding
                )
            }
        }
    }
}
