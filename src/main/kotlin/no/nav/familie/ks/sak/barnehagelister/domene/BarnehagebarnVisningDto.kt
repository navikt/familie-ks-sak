package no.nav.familie.ks.sak.barnehagelister.domene

import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
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
) {
    companion object {
        fun opprett(
            barnehagebarn: BarnehagebarnPaginerbar,
            fagsakId: Long? = null,
            fagsakStatus: FagsakStatus? = null,
        ) = BarnehagebarnVisningDto(
            ident = barnehagebarn.getIdent(),
            fom = barnehagebarn.getFom(),
            tom = barnehagebarn.getTom(),
            antallTimerBarnehage = barnehagebarn.getAntallTimerBarnehage(),
            endringstype = barnehagebarn.getEndringstype(),
            kommuneNavn = barnehagebarn.getKommuneNavn(),
            kommuneNr = barnehagebarn.getKommuneNr(),
            fagsakId = fagsakId,
            fagsakstatus = fagsakStatus.toString(),
            endretTid = barnehagebarn.getEndretTid(),
            avvik = barnehagebarn.getAvvik(),
        )
    }
}
