package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import java.math.BigDecimal

/**
 * Kalkulerer nytt utbetalingsbeløp fra [utenlandskPeriodebeløpINorskeKroner]
 * Beløpet konverteres fra desimaltall til heltall ved å strippe desimalene, og dermed øke den norske ytelsen med inntil én krone
 * Må håndtere tilfellet der [kalkulertUtebetalngsbeløp] blir modifisert andre steder i koden, men antar at det aldri vil være negativt
 * [nasjonaltPeriodebeløp] settes til den originale, nasjonale beregningen (aldri negativt)
 * [differanseberegnetBeløp] er differansen mellom [nasjonaltPeriodebeløp] og (avrundet) [utenlandskPeriodebeløpINorskeKroner] (kan bli negativt)
 * [kalkulertUtebetalngsbeløp] blir satt til [differanseberegnetBeløp], med mindre det er negativt. Da blir det 0 (null)
 * Hvis [utenlandskPeriodebeløpINorskeKroner] er <null>, så skal utbetalingsbeløpet reverteres til det originale nasjonale beløpet
 */
fun AndelTilkjentYtelse?.oppdaterDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal?,
): AndelTilkjentYtelse? {
    val nyAndelTilkjentYtelse =
        when {
            this == null -> null
            utenlandskPeriodebeløpINorskeKroner == null -> this.utenDifferanseberegning()
            else -> this.medDifferanseberegning(utenlandskPeriodebeløpINorskeKroner)
        }

    return nyAndelTilkjentYtelse
}

fun AndelTilkjentYtelse.medDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal,
): AndelTilkjentYtelse {
    val avrundetUtenlandskPeriodebeløp =
        utenlandskPeriodebeløpINorskeKroner
            .toBigInteger()
            .intValueExact() // Fjern desimaler for å gi fordel til søker

    val nyttDifferanseberegnetBeløp =
        (
            nasjonaltPeriodebeløp
                ?: kalkulertUtbetalingsbeløp
        ) - avrundetUtenlandskPeriodebeløp

    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        differanseberegnetPeriodebeløp = nyttDifferanseberegnetBeløp,
    )
}

private fun AndelTilkjentYtelse.utenDifferanseberegning(): AndelTilkjentYtelse =
    copy(
        id = 0,
        kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp ?: this.kalkulertUtbetalingsbeløp,
        differanseberegnetPeriodebeløp = null,
    )
