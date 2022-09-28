package no.nav.familie.ks.sak.common.util

object Utils {

    fun Any?.nullableTilString() = this?.toString() ?: ""

    fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }
}
