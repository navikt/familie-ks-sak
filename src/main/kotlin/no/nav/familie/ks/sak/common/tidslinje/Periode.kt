@file:Suppress("UNCHECKED_CAST")

package no.nav.familie.ks.sak.common.tidslinje

import java.time.LocalDate

data class Periode<T>(
    val verdi: T,
    val fom: LocalDate?,
    val tom: LocalDate?
) {

    fun tilTidslinjePeriodeMedDato() = TidslinjePeriodeMedDato(verdi, fom, tom)
}

fun <T> List<Periode<T>>.tilTidslinje(): Tidslinje<T> = this.map { it.tilTidslinjePeriodeMedDato() }
    .sortedBy { it.fom }.tilTidslinje()

fun <T> List<Periode<T>>.filtrerIkkeNull(): List<Periode<T & Any>> =
    this.mapNotNull { periode -> periode.verdi?.let { periode as Periode<T & Any> } }

data class IkkeNullbarPeriode<T>(val verdi: T, val fom: LocalDate, val tom: LocalDate)
