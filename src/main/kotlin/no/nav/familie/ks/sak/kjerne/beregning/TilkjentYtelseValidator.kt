package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.common.exception.UtbetalingsikkerhetFeil
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

object TilkjentYtelseValidator {

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ) {
        val søker = personopplysningGrunnlag.søker
        val barna = personopplysningGrunnlag.barna

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        tidslinjeMedAndeler.tilPerioder().forEach {
            val søkersAndeler = hentSøkersAndeler(it.verdi!!, søker)
            val barnasAndeler = hentBarnasAndeler(it.verdi, barna)

            validerAtBeløpForPartStemmerMedSatser(søker, søkersAndeler)

            barnasAndeler.forEach { (person, andeler) ->
                validerAtBeløpForPartStemmerMedSatser(person, andeler)
            }
        }
    }

    private fun hentSøkersAndeler(
        andeler: List<AndelTilkjentYtelse>,
        søker: Person
    ) = andeler.filter { it.aktør == søker.aktør }

    private fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
        barn to andeler.filter { it.aktør == barn.aktør }
    }

    private fun validerAtBeløpForPartStemmerMedSatser(person: Person, andeler: List<AndelTilkjentYtelse>) {
        val maksAntallAndeler = if (person.type == PersonType.BARN) 1 else 2
        val maksTotalBeløp = maksBeløp()

        val feilMelding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet"
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
