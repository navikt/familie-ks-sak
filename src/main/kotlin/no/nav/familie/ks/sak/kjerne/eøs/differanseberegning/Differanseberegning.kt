package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.tilKronerPerValutaenhet
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.tidslinje.utvidelser.outerJoin

/**
 * ADVARSEL: Muterer TilkjentYtelse
 * Denne BURDE gjøres ikke-muterbar og returnere en ny instans av TilkjentYtelse
 * Muteringen skyldes at TilkjentYtelse er under JPA-kontekst og ikke "tåler" copy(andelerTilkjentYtelse = ...)
 * Starten på én løsning er at EndretUtebetalingPeriode kobles løs fra AndelTilkjentYtelse og kobles rett på behandlingen
 */
fun beregnDifferanse(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    utenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
    valutakurser: List<Valutakurs>,
): List<AndelTilkjentYtelse> {
    val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
    val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()
    val andelTilkjentYtelseTidslinjer = andelerTilkjentYtelse.tilSeparateTidslinjerForBarna()

    val barnasUtenlandskePeriodebeløpINorskeKronerTidslinjer =
        utenlandskePeriodebeløpTidslinjer.outerJoin(valutakursTidslinjer) { upb, valutakurs ->
            upb.tilMånedligValutabeløp() * valutakurs.tilKronerPerValutaenhet()
        }

    val barnasDifferanseberegneteAndelTilkjentYtelseTidslinjer =
        andelTilkjentYtelseTidslinjer.outerJoin(barnasUtenlandskePeriodebeløpINorskeKronerTidslinjer) { aty, beløp ->
            aty.oppdaterDifferanseberegning(beløp)
        }

    return barnasDifferanseberegneteAndelTilkjentYtelseTidslinjer.tilAndelerTilkjentYtelse()
}
