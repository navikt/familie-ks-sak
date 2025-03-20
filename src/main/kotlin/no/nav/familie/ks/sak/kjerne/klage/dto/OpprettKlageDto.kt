package no.nav.familie.ks.sak.kjerne.klage.dto

import java.time.LocalDate

data class OpprettKlageDto(
    val kravMottattDato: LocalDate?,
    val klageMottattDato: LocalDate?,
)
