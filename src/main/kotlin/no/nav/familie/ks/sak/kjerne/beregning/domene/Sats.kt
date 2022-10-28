package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.common.util.toYearMonth
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

data class Sats(
    val type: SatsType,
    val beløp: Int,
    val gyldigFom: LocalDate = LocalDate.MIN,
    val gyldigTom: LocalDate = LocalDate.MAX
)

enum class SatsType(val beskrivelse: String) {
    KS("Ordinær kontantstøtte"),
}

data class SatsPeriode(val sats: Int, val fom: YearMonth, val tom: YearMonth, val prosent: BigDecimal)

private val sats = Sats(type = SatsType.KS, beløp = 7500, gyldigFom = LocalDate.MIN, gyldigTom = LocalDate.MAX)

fun maksBeløp() = sats.beløp

fun hentGyldigSatsFor(antallTimer: BigDecimal?, erDeltBosted: Boolean, stønadFom: YearMonth, stønadTom: YearMonth): SatsPeriode {
    val prosent = when {
        erDeltBosted -> 50
        antallTimer == null -> 100
        antallTimer > BigDecimal(0) && antallTimer <= BigDecimal(8) -> 80
        antallTimer >= BigDecimal(9) && antallTimer <= BigDecimal(16) -> 60
        antallTimer >= BigDecimal(17) && antallTimer <= BigDecimal(24) -> 40
        antallTimer >= BigDecimal(25) && antallTimer <= BigDecimal(32) -> 20
        else -> 0
    }
    return SatsPeriode(
        sats = sats.beløp,
        fom = maxOf(sats.gyldigFom.toYearMonth(), stønadFom),
        tom = minOf(sats.gyldigTom.toYearMonth(), stønadTom),
        prosent = prosent.toBigDecimal()
    )
}

fun Int.prosent(prosent: BigDecimal) =
    this.toBigDecimal().times(prosent).divide(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toInt()
