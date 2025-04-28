package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class BarnehagebarnVisningDto(
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val antallTimerBarnehage: Double,
    val endringstype: String? = null,
    val kommuneNavn: String,
    val kommuneNr: String,
    val fagsakId: Long? = null,
    val fagsakstatus: String? = null,
    val endretTid: LocalDateTime,
    val avvik: Boolean? = null,
)

interface BarnehagebarnForListe {
    fun getIdent(): String

    fun getFom(): LocalDate

    fun getTom(): LocalDate?

    fun getAntallTimerBarnehage(): Double

    fun getEndringstype(): String?

    fun getKommuneNavn(): String

    fun getKommuneNr(): String

    fun getEndretTid(): LocalDateTime

    fun getAvvik(): Boolean?
}
