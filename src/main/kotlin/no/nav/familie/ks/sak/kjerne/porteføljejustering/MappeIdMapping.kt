package no.nav.familie.ks.sak.kjerne.porteføljejustering

import no.nav.familie.ks.sak.common.exception.Feil

private val mappeIdVadsøTilBergen =
    mapOf(
        100012691 to 100012789,
        100012692 to 100012790,
        100012693 to 100012791,
        100012721 to 100012792,
        100012695 to 100012765,
    )

fun hentMappeIdHosBergenSomTilsvarerMappeIVadsø(
    mappeIdVadsø: Int,
): Int =
    mappeIdVadsøTilBergen
        .getOrElse(mappeIdVadsø) { throw Feil("Finner ikke mappe id $mappeIdVadsø i mapping") }
