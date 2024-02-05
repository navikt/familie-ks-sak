package no.nav.familie.ks.sak.integrasjon.pdl.domene

data class PdlPersonRequest(
    val variables: PdlPersonRequestVariables,
    val query: String,
)

data class PdlPersonBolkRequest(
    val variables: PdlPersonBolkRequestVariables,
    val query: String,
)

data class PdlPersonRequestVariables(var ident: String)
data class PdlPersonBolkRequestVariables(var identer: List<String>)
