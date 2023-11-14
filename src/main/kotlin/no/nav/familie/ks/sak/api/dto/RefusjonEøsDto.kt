package no.nav.familie.ks.sak.api.dto

import java.time.LocalDate

data class RefusjonEøsDto(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val refusjonsbeløp: Int,
    val land: String,
    val refusjonAvklart: Boolean,
)
