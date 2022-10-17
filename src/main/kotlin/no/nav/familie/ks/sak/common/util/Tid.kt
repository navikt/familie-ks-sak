package no.nav.familie.ks.sak.common.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val nbLocale = Locale("nb", "Norway")

val TIDENES_MORGEN = LocalDate.MIN

fun LocalDate.tilddMMyy() = this.format(DateTimeFormatter.ofPattern("ddMMyy", nbLocale))
fun LocalDate.tilyyyyMMdd() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", nbLocale))
fun LocalDate.tilKortString() = this.format(DateTimeFormatter.ofPattern("dd.MM.yy", nbLocale))
fun LocalDate.tilDagMånedÅr() = this.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", nbLocale))
fun LocalDate.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))
fun LocalDate.tilYearMonth() = YearMonth.from(this)
fun LocalDate.sisteDagIMåned(): LocalDate = YearMonth.from(this).atEndOfMonth()
fun YearMonth.tilKortString() = this.format(DateTimeFormatter.ofPattern("MM.yy", nbLocale))
fun YearMonth.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))
fun YearMonth.toLocalDate() = LocalDate.of(this.year, this.month, 1)

fun LocalDate.toYearMonth() = YearMonth.from(this)
fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)
fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()

fun YearMonth.erSammeEllerTidligere(toCompare: YearMonth): Boolean {
    return this.isBefore(toCompare) || this == toCompare
}

fun inneværendeMåned(): YearMonth {
    return LocalDate.now().toYearMonth()
}

fun LocalDate.nesteMåned(): YearMonth {
    return this.toYearMonth().plusMonths(1)
}

fun YearMonth.nesteMåned(): YearMonth {
    return this.plusMonths(1)
}

fun LocalDate.erDagenFør(other: LocalDate?) = other != null && this.plusDays(1).equals(other)

data class Periode(val fom: LocalDate, val tom: LocalDate)

fun LocalDate.erSammeEllerEtter(toCompare: LocalDate): Boolean {
    return this.isAfter(toCompare) || this == toCompare
}

fun LocalDate.erMellom(toCompare: Periode): Boolean {
    return this.erSammeEllerEtter(toCompare.fom) && this.erSammeEllerEtter(toCompare.tom)
}

fun Periode.overlapperHeltEllerDelvisMed(annenPeriode: Periode) =
    this.fom.erMellom(annenPeriode) ||
        this.tom.erMellom(annenPeriode) ||
        annenPeriode.fom.erMellom(this) ||
        annenPeriode.tom.erMellom(this)


data class MånedPeriode(val fom: YearMonth, val tom: YearMonth)

fun MånedPeriode.inkluderer(yearMonth: YearMonth) = yearMonth >= this.fom && yearMonth <= this.tom
fun MånedPeriode.overlapperHeltEllerDelvisMed(annenPeriode: MånedPeriode) =
    this.inkluderer(annenPeriode.fom) ||
        this.inkluderer(annenPeriode.tom) ||
        annenPeriode.inkluderer(this.fom) ||
        annenPeriode.inkluderer(this.tom)
