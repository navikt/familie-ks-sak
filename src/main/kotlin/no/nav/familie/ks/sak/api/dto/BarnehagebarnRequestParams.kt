package no.nav.familie.ks.sak.api.dto

data class BarnehagebarnRequestParams(
    val ident: String?,
    val kommuneNavn: String?,
    val kunLøpendeFagsak: Boolean,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortBy: String = "kommuneNavn",
    val sortAsc: Boolean = false,
    val kunLøpendeAndel: Boolean = false,
)
