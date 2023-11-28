package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene

import no.nav.familie.ks.sak.common.util.del
import no.nav.familie.ks.sak.common.util.multipliser
import java.math.BigDecimal

enum class Intervall {
    ÅRLIG,
    KVARTALSVIS,
    MÅNEDLIG,
    UKENTLIG,
}

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal): BigDecimal =
    when (this) {
        Intervall.ÅRLIG -> beløp.del(12.toBigDecimal(), 10)
        Intervall.KVARTALSVIS -> beløp.del(3.toBigDecimal(), 10)
        Intervall.MÅNEDLIG -> beløp
        Intervall.UKENTLIG -> beløp.multipliser(4.35.toBigDecimal(), 10)
    }.stripTrailingZeros().toPlainString().toBigDecimal()
