package no.nav.familie.ks.sak.common.util

import no.nav.familie.kontrakter.felles.objectMapper
import java.text.NumberFormat

fun Any?.nullableTilString() = this?.toString() ?: ""

fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }

inline fun <reified T : Enum<T>> konverterEnumsTilString(liste: List<T>) = liste.joinToString(separator = ";")

inline fun <reified T : Enum<T>> konverterStringTilEnums(string: String?): List<T> =
    if (string.isNullOrBlank()) emptyList() else string.split(";").map { enumValueOf(it) }

fun slåSammen(stringListe: List<String>): String = Regex("(.*),").replace(stringListe.joinToString(", "), "$1 og")

fun Any.convertDataClassToJson(): String = objectMapper.writeValueAsString(this)

fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)
