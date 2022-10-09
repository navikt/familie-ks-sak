package no.nav.familie.ks.sak.common.util

fun Any?.nullableTilString() = this?.toString() ?: ""

fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }

inline fun <reified T : Enum<T>> konverterEnumsTilString(liste: List<T>) = liste.joinToString(separator = ";")

inline fun <reified T : Enum<T>> konverterStringTilEnums(string: String?): List<T> =
    if (string.isNullOrBlank()) emptyList() else string.split(";").map { enumValueOf(it) }
