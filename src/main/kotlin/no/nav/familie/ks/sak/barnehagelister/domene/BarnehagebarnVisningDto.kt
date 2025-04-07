package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class BarnehagebarnVisningDto(
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val antallTimerIBarnehage: Double,
    val endringstype: String? = null,
    val kommuneNavn: String,
    val kommuneNr: String,
    val fagsakId: Long? = null,
    val fagsakstatus: String? = null,
    val endretTid: LocalDateTime,
    val avvik: Boolean? = null,
)
