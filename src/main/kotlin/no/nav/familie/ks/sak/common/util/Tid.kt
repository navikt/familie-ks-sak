package no.nav.familie.ks.sak.common.util

import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val nbLocale = Locale.of("nb", "Norway")

val TIDENES_MORGEN = LocalDate.MIN
val TIDENES_ENDE = LocalDate.MAX
val MAX_MÅNED = LocalDate.MAX.toYearMonth()
val MIN_MÅNED = LocalDate.MIN.toYearMonth()

val DATO_LOVENDRING_2024 = LocalDate.of(2024, Month.AUGUST, 1)

fun LocalDate.tilddMMyy() = this.format(DateTimeFormatter.ofPattern("ddMMyy", nbLocale))

fun LocalDate.tilyyyyMMdd() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", nbLocale))

fun LocalDate.tilKortString() = this.format(DateTimeFormatter.ofPattern("dd.MM.yy", nbLocale))

fun LocalDate.tilddMMyyyy() = this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", nbLocale))

fun LocalDate.tilDagMånedÅr() = this.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", nbLocale))

fun LocalDate.tilDagMånedÅrKort() = this.format(DateTimeFormatter.ofPattern("d. MMM yy", nbLocale))

fun LocalDate.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))

fun LocalDate.tilYearMonth() = YearMonth.from(this)

fun LocalDate.sisteDagIMåned(): LocalDate = YearMonth.from(this).atEndOfMonth()

fun YearMonth.tilKortString() = this.format(DateTimeFormatter.ofPattern("MM.yy", nbLocale))

fun YearMonth.tilMånedÅrKort() = this.format(DateTimeFormatter.ofPattern("MMM yyy", nbLocale))

fun YearMonth.toLocalDate() = LocalDate.of(this.year, this.month, 1)

fun LocalDate.toYearMonth() = YearMonth.from(this)

fun YearMonth.førsteDagIInneværendeMåned() = this.atDay(1)

fun YearMonth.sisteDagIInneværendeMåned() = this.atEndOfMonth()

fun LocalDate.erSenereEnnInneværendeMåned(): Boolean = this.isAfter(LocalDate.now().sisteDagIMåned())

fun LocalDate.erSenereEnnNesteMåned(): Boolean = this.isAfter(LocalDate.now().plusMonths(1).sisteDagIMåned())

fun inneværendeMåned(): YearMonth = LocalDate.now().toYearMonth()

fun YearMonth.forrigeMåned(): YearMonth = this.minusMonths(1)

fun YearMonth.nesteMåned(): YearMonth = this.plusMonths(1)

fun YearMonth.isSameOrAfter(toCompare: YearMonth): Boolean = this.isAfter(toCompare) || this == toCompare

fun LocalDate.erDagenFør(other: LocalDate?) = other != null && this.plusDays(1).equals(other)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

fun LocalDate.erSammeEllerFør(toCompare: LocalDate): Boolean = this.isBefore(toCompare) || this == toCompare

fun LocalDate.erSammeEllerEtter(toCompare: LocalDate): Boolean = this.isAfter(toCompare) || this == toCompare

fun LocalDate.erFørsteAugust2024EllerSenere(): Boolean = !this.isBefore(LocalDate.of(2024, 8, 1))

fun LocalDate.førsteDagIInneværendeMåned() = this.withDayOfMonth(1)

data class MånedPeriode(
    val fom: YearMonth,
    val tom: YearMonth,
)

data class NullablePeriode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

fun MånedPeriode.inkluderer(yearMonth: YearMonth) = yearMonth >= this.fom && yearMonth <= this.tom

fun MånedPeriode.overlapperHeltEllerDelvisMed(annenPeriode: MånedPeriode) =
    this.inkluderer(annenPeriode.fom) ||
        this.inkluderer(annenPeriode.tom) ||
        annenPeriode.inkluderer(this.fom) ||
        annenPeriode.inkluderer(this.tom)

fun MånedPeriode.erMellom(annenPeriode: MånedPeriode) = annenPeriode.inkluderer(this.fom) && annenPeriode.inkluderer(this.tom)

fun MånedPeriode.antallMåneder(): Int = fom.until(tom, ChronoUnit.MONTHS).toInt() + 1

fun MånedPeriode.månederIPeriode(): List<YearMonth> = generateSequence(fom) { it.plusMonths(1) }.takeWhile { it <= tom }.toList()

fun LocalDate.førsteDagINesteMåned() = this.plusMonths(1).withDayOfMonth(1)

fun erBack2BackIMånedsskifte(
    tilOgMed: LocalDate?,
    fraOgMed: LocalDate?,
): Boolean = tilOgMed?.erDagenFør(fraOgMed) == true && tilOgMed.toYearMonth() != fraOgMed?.toYearMonth()

fun DatoIntervallEntitet.erInnenfor(dato: LocalDate): Boolean =
    when {
        fom == null && tom == null -> true
        fom == null -> dato.erSammeEllerFør(tom!!)
        tom == null -> dato.erSammeEllerEtter(fom)
        else -> dato.erSammeEllerEtter(fom) && dato.erSammeEllerFør(tom)
    }

fun LocalDateTime.erHverdag(): Boolean = this.dayOfWeek != DayOfWeek.SATURDAY && this.dayOfWeek != DayOfWeek.SUNDAY

fun erKlokkenMellom21Og06(localTime: LocalTime = LocalTime.now()): Boolean = localTime.isAfter(LocalTime.of(21, 0)) || localTime.isBefore(LocalTime.of(6, 0))

fun kl06IdagEllerNesteDag(date: LocalDateTime = LocalDateTime.now()): LocalDateTime =
    if (date.toLocalTime().isBefore(LocalTime.of(6, 0))) {
        date.withHour(6)
    } else {
        date.plusDays(1).withHour(6)
    }

fun LocalDate.erHelligdag() =
    this.dayOfMonth == 1 &&
        this.month == Month.JANUARY ||
        this.dayOfMonth == 1 &&
        this.month == Month.MAY ||
        this.dayOfMonth == 17 &&
        this.month == Month.MAY ||
        this.dayOfMonth == 25 &&
        this.month == Month.DECEMBER ||
        this.dayOfMonth == 26 &&
        this.month == Month.DECEMBER
