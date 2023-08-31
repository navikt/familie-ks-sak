package no.nav.familie.ks.sak.integrasjon.pdl.domene

data class PdlPersonRequest(
    val variables: PdlPersonRequestVariables,
    val query: String,
)

data class PdlPersonRequestVariables(var ident: String)
