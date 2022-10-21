package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
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

            // TODO Sats validering
        }
    }

    fun hentSøkersAndeler(
        andeler: List<AndelTilkjentYtelse>,
        søker: Person
    ) = andeler.filter { it.aktør == søker.aktør }

    fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<Person>) = barna.map { barn ->
        barn to andeler.filter { it.aktør == barn.aktør }
    }
}
