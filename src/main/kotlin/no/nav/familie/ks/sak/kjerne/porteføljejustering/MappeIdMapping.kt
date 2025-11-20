package no.nav.familie.ks.sak.kjerne.porteføljejustering

import no.nav.familie.ks.sak.common.exception.Feil

private val mappeIdVadsøTilBergen =
    mapOf(
        100012691L to "100012789",
        100012692L to "100012790",
        100012693L to "100012791",
        100012721L to "100012792",
        100012695L to "100012765",
    )

fun hentMappeIdHosBergenSomTilsvarerMappeIVadsø(
    mappeIdVadsø: Long,
): String =
    mappeIdVadsøTilBergen
        .getOrElse(mappeIdVadsø) { throw Feil("Finner ikke mappe id $mappeIdVadsø i mapping") }
