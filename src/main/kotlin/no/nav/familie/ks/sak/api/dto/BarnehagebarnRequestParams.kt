package no.nav.familie.ks.sak.api.dto

data class BarnehagebarnRequestParams(
    val ident: String?,
    val fom: String?,
    val tom: String?,
    val endringstype: String?,
    val kommuneNavn: String?,
    val kommuneNr: String?,
    val antallTimerIBarnehage: Int?,
    val kunLøpendeFagsak: Boolean,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortBy: String = "endret_tid",
    val sortAsc: Boolean = false,
)
