package no.nav.familie.ks.sak.common.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val nbLocale = Locale("nb", "Norway")

fun LocalDate.tilddMMyy() = this.format(DateTimeFormatter.ofPattern("ddMMyy", nbLocale))
fun LocalDate.tilyyyyMMdd() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", nbLocale))
fun LocalDate.tilKortString() = this.format(DateTimeFormatter.ofPattern("dd.MM.yy", nbLocale))
fun YearMonth.tilKortString() = this.format(DateTimeFormatter.ofPattern("MM.yy", nbLocale))
fun LocalDate.tilDagMånedÅr() = this.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", nbLocale))
fun LocalDate.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))
fun YearMonth.tilMånedÅr() = this.format(DateTimeFormatter.ofPattern("MMMM yyyy", nbLocale))
