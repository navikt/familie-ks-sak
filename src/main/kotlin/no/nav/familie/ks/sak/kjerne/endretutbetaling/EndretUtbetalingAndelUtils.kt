package no.nav.familie.ks.sak.kjerne.endretutbetaling

import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import java.time.YearMonth

fun beregnGyldigTomIFremtiden(
    andreEndredeAndelerPåBehandling: List<EndretUtbetalingAndel>,
    endretUtbetalingAndel: EndretUtbetalingAndel,
    andelTilkjentYtelser: List<AndelTilkjentYtelse>
): YearMonth? {
    val førsteEndringEtterDenneEndringen = andreEndredeAndelerPåBehandling.filter {
        it.fom?.isAfter(endretUtbetalingAndel.fom) == true &&
            it.person == endretUtbetalingAndel.person
    }.sortedBy { it.fom }.firstOrNull()

    return if (førsteEndringEtterDenneEndringen != null) {
        førsteEndringEtterDenneEndringen.fom?.minusMonths(1)
    } else {
        val sisteTomAndeler = andelTilkjentYtelser.filter {
            it.aktør == endretUtbetalingAndel.person?.aktør
        }.maxOf { it.stønadTom }

        sisteTomAndeler
    }
}
