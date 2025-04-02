package no.nav.familie.ks.sak.common.util

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

fun Any?.nullableTilString() = this?.toString() ?: ""

fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }

fun String.storForbokstavIAlleNavn() =
    this
        .split(" ")
        .joinToString(" ") { navn ->
            navn.split("-").joinToString("-") { it.storForbokstav() }
        }.trimEnd()

inline fun <reified T : Enum<T>> konverterEnumsTilString(liste: List<T>) = liste.joinToString(separator = ";")

inline fun <reified T : Enum<T>> konverterStringTilEnums(string: String?): List<T> = if (string.isNullOrBlank()) emptyList() else string.split(";").map { enumValueOf(it) }

fun slåSammen(stringListe: List<String>): String = Regex("(.*),").replace(stringListe.joinToString(", "), "$1 og")

fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

fun Int.avrundetHeltallAvProsent(prosent: BigDecimal) = this.toBigDecimal().avrundetHeltallAvProsent(prosent)

fun BigDecimal.avrundetHeltallAvProsent(prosent: BigDecimal) =
    this
        .times(prosent)
        .divide(100.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()

fun er11Siffer(ident: String): Boolean = ident.all { it.isDigit() } && ident.length == 11

fun formaterIdent(ident: String): String =
    when {
        er11Siffer(ident) -> "${ident.substring(0, 6)} ${ident.substring(6)}"
        else -> ident
    }

fun hentDokument(dokumentNavn: String): ByteArray {
    val dokumentByteArray = (
        {}::class.java.classLoader
            .getResourceAsStream("dokumenter/$dokumentNavn")
            ?.readAllBytes()
            ?: error("Klarte ikke hente dokument $dokumentNavn")
    )

    return dokumentByteArray
}

fun <T> toPage(
    list: List<T>,
    pageable: Pageable,
    fieldExtractor: (T) -> Comparable<*>?,
): Page<T> {
    val sortedList =
        if (pageable.sort.isSorted) {
            val order = pageable.sort.first() // Only take the first sorting field
            val comparator =
                if (order.isAscending) {
                    compareBy(fieldExtractor)
                } else {
                    compareByDescending(fieldExtractor)
                }
            list.sortedWith(comparator)
        } else {
            list
        }

    val start = (pageable.pageNumber * pageable.pageSize).coerceAtMost(sortedList.size)
    val end = (start + pageable.pageSize).coerceAtMost(sortedList.size)

    return PageImpl(sortedList.subList(start, end), pageable, sortedList.size.toLong())
}
