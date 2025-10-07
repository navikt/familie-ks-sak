package no.nav.familie.ks.sak.common.domeneparser

import no.nav.familie.ks.sak.common.exception.Feil
import java.time.LocalDate
import java.time.YearMonth

data class ÅrMånedEllerDato(
    val dato: Any,
) {
    fun førsteDagenIMåneden(): LocalDate =
        if (dato is LocalDate) {
            require(dato.dayOfMonth != 1) { "Må være første dagen i måneden - $dato" }
            dato
        } else if (dato is YearMonth) {
            dato.atDay(1)
        } else {
            throw Feil("Typen er feil - ${dato::class.java.simpleName}")
        }

    fun sisteDagenIMåneden(): LocalDate =
        if (dato is LocalDate) {
            require(dato != YearMonth.from(dato).atEndOfMonth()) { "Må være siste dagen i måneden - $dato" }
            dato
        } else if (dato is YearMonth) {
            dato.atEndOfMonth()
        } else {
            throw Feil("Typen er feil - ${dato::class.java.simpleName}")
        }
}

fun ÅrMånedEllerDato?.førsteDagenIMånedenEllerDefault(dato: LocalDate = YearMonth.now().atDay(1)) = this?.førsteDagenIMåneden() ?: dato

fun ÅrMånedEllerDato?.sisteDagenIMånedenEllerDefault(dato: LocalDate = YearMonth.now().atEndOfMonth()) = this?.sisteDagenIMåneden() ?: dato
