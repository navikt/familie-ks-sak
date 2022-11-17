package no.nav.familie.ks.sak.common.util

import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val nbLocale = Locale("nb", "Norway")

val TIDENES_MORGEN = LocalDate.MIN
val TIDENES_ENDE = LocalDate.MAX

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

fun YearMonth.erSammeEllerTidligere(toCompare: YearMonth): Boolean = this.isBefore(toCompare) || this == toCompare
fun LocalDate.erSenereEnnInneværendeMåned(): Boolean = this.isAfter(LocalDate.now().sisteDagIMåned())

fun inneværendeMåned(): YearMonth = LocalDate.now().toYearMonth()

fun YearMonth.forrigeMåned(): YearMonth = this.minusMonths(1)
fun YearMonth.nesteMåned(): YearMonth = this.plusMonths(1)

fun LocalDate.erDagenFør(other: LocalDate?) = other != null && this.plusDays(1).equals(other)

data class Periode(val fom: LocalDate, val tom: LocalDate)

fun LocalDate.erSammeEllerFør(toCompare: LocalDate): Boolean = this.isBefore(toCompare) || this == toCompare
fun LocalDate.erSammeEllerEtter(toCompare: LocalDate): Boolean = this.isAfter(toCompare) || this == toCompare
fun LocalDate.erMellom(toCompare: Periode): Boolean = this.erSammeEllerEtter(toCompare.fom) &&
    this.erSammeEllerEtter(toCompare.tom)

fun LocalDate.førsteDagIInneværendeMåned() = this.withDayOfMonth(1)

fun Periode.overlapperHeltEllerDelvisMed(annenPeriode: Periode) =
    this.fom.erMellom(annenPeriode) ||
        this.tom.erMellom(annenPeriode) ||
        annenPeriode.fom.erMellom(this) ||
        annenPeriode.tom.erMellom(this)

fun Periode.tilMånedPeriode(): MånedPeriode = MånedPeriode(fom = this.fom.toYearMonth(), tom = this.tom.toYearMonth())

data class MånedPeriode(val fom: YearMonth, val tom: YearMonth)

data class NullablePeriode(val fom: LocalDate?, val tom: LocalDate?)

data class NullableMånedPeriode(val fom: YearMonth?, val tom: YearMonth?)

fun MånedPeriode.inkluderer(yearMonth: YearMonth) = yearMonth >= this.fom && yearMonth <= this.tom
fun MånedPeriode.overlapperHeltEllerDelvisMed(annenPeriode: MånedPeriode) =
    this.inkluderer(annenPeriode.fom) ||
        this.inkluderer(annenPeriode.tom) ||
        annenPeriode.inkluderer(this.fom) ||
        annenPeriode.inkluderer(this.tom)

fun MånedPeriode.erMellom(annenPeriode: MånedPeriode) =
    annenPeriode.inkluderer(this.fom) && annenPeriode.inkluderer(this.tom)

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)

fun erBack2BackIMånedsskifte(tilOgMed: LocalDate?, fraOgMed: LocalDate?): Boolean =
    tilOgMed?.erDagenFør(fraOgMed) == true && tilOgMed.toYearMonth() != fraOgMed?.toYearMonth()

fun DatoIntervallEntitet.erInnenfor(dato: LocalDate): Boolean =
    when {
        fom == null && tom == null -> true
        fom == null -> dato.erSammeEllerFør(tom!!)
        tom == null -> dato.erSammeEllerEtter(fom)
        else -> dato.erSammeEllerEtter(fom) && dato.erSammeEllerFør(tom)
    }

fun LocalDateTime.erHverdag(plusDays: Long = 0): Boolean {
    val dayOfWeek = plusDays(plusDays).dayOfWeek

    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
}

fun erKlokkenMellom21Og06(localTime: LocalTime = LocalTime.now()): Boolean =
    localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))

fun kl06IdagEllerNesteDag(date: LocalDateTime = LocalDateTime.now()): LocalDateTime {
    return if (date.toLocalTime().isBefore(LocalTime.of(6, 0))) {
        date.withHour(6)
    } else {
        date.plusDays(1).withHour(6)
    }
}
