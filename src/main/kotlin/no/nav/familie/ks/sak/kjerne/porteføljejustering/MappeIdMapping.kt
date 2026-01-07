package no.nav.familie.ks.sak.kjerne.porteføljejustering

import no.nav.familie.ks.sak.common.exception.Feil

private val mappeIdVadsøTilBergen =
    mapOf(
        100012691L to 100012789L,
        100012692L to 100012790L,
        100012693L to 100012791L,
        100012721L to 100012792L,
        100012695L to 100012765L,
    )

fun hentMappeIdHosBergenSomTilsvarerMappeIVadsø(
    mappeIdVadsø: Long?,
): Long? =
    mappeIdVadsø?.let {
        mappeIdVadsøTilBergen
            .getOrElse(mappeIdVadsø) { throw Feil("Finner ikke mappe id $mappeIdVadsø i mapping") }
    }
