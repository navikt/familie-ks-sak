package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate

data class BarnehagebarnInfotrygdDto(
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val antallTimerIBarnehage: Double,
    val endringstype: String,
    val kommuneNavn: String,
    val kommuneNr: String,
    val harFagsak: Boolean,
)
