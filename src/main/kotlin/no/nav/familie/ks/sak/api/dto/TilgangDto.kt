package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class TilgangRequestDto(val brukerIdent: String)

data class TilgangResponsDto(
    val saksbehandlerHarTilgang: Boolean,
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING,
)
