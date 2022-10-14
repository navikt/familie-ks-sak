package no.nav.familie.ks.sak.integrasjon.sanity.domene

data class SanityEØSBegrunnelse(
    val apiNavn: String,
    val navnISystem: String
)

data class SanityEØSBegrunnelserResponsDto(
    val ms: Int,
    val query: String,
    val result: List<SanityEØSBegrunnelseDto>
)

// TODO: Har fjernet de fleste av feltene som brukes i ba-sak, så her må vi finne ut hvilke felter vi skal ha for KS
data class SanityEØSBegrunnelseDto(
    val apiNavn: String,
    val navnISystem: String
) {
    fun tilSanityEØSBegrunnelse(): SanityEØSBegrunnelse {
        return SanityEØSBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem
        )
    }
}
