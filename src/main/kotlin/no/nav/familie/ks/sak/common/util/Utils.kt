package no.nav.familie.ks.sak.common.util

import no.nav.familie.kontrakter.felles.objectMapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

fun Any?.nullableTilString() = this?.toString() ?: ""

fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }
fun String.storForbokstavIAlleNavn() = this.split(" ")
    .joinToString(" ") { navn ->
        navn.split("-").joinToString("-") { it.storForbokstav() }
    }.trimEnd()

inline fun <reified T : Enum<T>> konverterEnumsTilString(liste: List<T>) = liste.joinToString(separator = ";")

inline fun <reified T : Enum<T>> konverterStringTilEnums(string: String?): List<T> =
    if (string.isNullOrBlank()) emptyList() else string.split(";").map { enumValueOf(it) }

fun slåSammen(stringListe: List<String>): String = Regex("(.*),").replace(stringListe.joinToString(", "), "$1 og")

fun Any.convertDataClassToJson(): String = objectMapper.writeValueAsString(this)

fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

fun Int.avrundetHeltallAvProsent(prosent: BigDecimal) = this.toBigDecimal().avrundetHeltallAvProsent(prosent)

fun BigDecimal.avrundetHeltallAvProsent(prosent: BigDecimal) = this.times(prosent)
    .divide(100.toBigDecimal()).setScale(0, RoundingMode.HALF_UP)
    .toInt()

fun er11Siffer(ident: String): Boolean = ident.all { it.isDigit() } && ident.length == 11

fun formaterIdent(ident: String): String = when {
    er11Siffer(ident) -> "${ident.substring(0, 6)} ${ident.substring(6)}"
    else -> ident
}
