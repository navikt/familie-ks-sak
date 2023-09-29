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
) {

    companion object {
        fun fraBarnehageBarinInterfaceTilDto(
            barnehagebarnInfotrygdDtoInterface: BarnehagebarnInfotrygdDtoInterface,
            harFagsak: Boolean,
        ): BarnehagebarnInfotrygdDto {
            return BarnehagebarnInfotrygdDto(
                ident = barnehagebarnInfotrygdDtoInterface.getIdent(),
                fom = barnehagebarnInfotrygdDtoInterface.getFom(),
                tom = barnehagebarnInfotrygdDtoInterface.getTom(),
                antallTimerIBarnehage = barnehagebarnInfotrygdDtoInterface.getAntallTimerIBarnehage(),
                endringstype = barnehagebarnInfotrygdDtoInterface.getEndringstype(),
                kommuneNavn = barnehagebarnInfotrygdDtoInterface.getKommuneNavn(),
                kommuneNr = barnehagebarnInfotrygdDtoInterface.getKommuneNr(),
                harFagsak = harFagsak,
            )
        }
    }
}
