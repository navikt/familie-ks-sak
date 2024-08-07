package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class BarnehagebarnInfotrygdDto(
    val ident: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val antallTimerIBarnehage: Double,
    val endringstype: String,
    val kommuneNavn: String,
    val kommuneNr: String,
    val harFagsak: Boolean,
    val endretTid: LocalDateTime,
) {
    companion object {
        fun fraBarnehageBarnInterfaceTilDto(
            barnehagebarnInfotrygdDtoInterface: BarnehagebarnInfotrygdDtoInterface,
            harFagsak: Boolean,
        ): BarnehagebarnInfotrygdDto =
            BarnehagebarnInfotrygdDto(
                ident = barnehagebarnInfotrygdDtoInterface.getIdent(),
                fom = barnehagebarnInfotrygdDtoInterface.getFom(),
                tom = barnehagebarnInfotrygdDtoInterface.getTom(),
                antallTimerIBarnehage = barnehagebarnInfotrygdDtoInterface.getAntallTimerIBarnehage(),
                endringstype = barnehagebarnInfotrygdDtoInterface.getEndringstype(),
                kommuneNavn = barnehagebarnInfotrygdDtoInterface.getKommuneNavn(),
                kommuneNr = barnehagebarnInfotrygdDtoInterface.getKommuneNr(),
                harFagsak = harFagsak,
                endretTid = barnehagebarnInfotrygdDtoInterface.getEndretTid(),
            )
    }
}
