package no.nav.familie.ks.sak.common.util

fun Any?.nullableTilString() = this?.toString() ?: ""

fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }
