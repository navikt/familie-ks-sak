package no.nav.familie.ks.sak.config

import no.nav.familie.kontrakter.felles.Tema
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class PdlConfig(
    @Value("\${PDL_URL}") pdlUrl: URI,
) {
    val pdlUri: URI = UriComponentsBuilder.fromUri(pdlUrl).pathSegment(PATH_GRAPHQL).build().toUri()

    companion object {
        const val PATH_GRAPHQL = "graphql"

        val hentIdenterQuery = graphqlQuery("/pdl/hentIdenter.graphql")
        val hentAdressebeskyttelseQuery = graphqlQuery("/pdl/hent-adressebeskyttelse.graphql")
        val hentEnkelPersonQuery = graphqlQuery("/pdl/hentperson-enkel.graphql")
        val hentPersonMedRelasjonOgRegisterInformasjonQuery =
            graphqlQuery("/pdl/hentperson-med-relasjoner-og-registerinformasjon.graphql")
        val hentPersonMedNavnOgAdresseQuery = graphqlQuery("/pdl/hentperson-navn-og-adresse.graphql")
        val hentStatsborgerskapUtenHistorikkQuery = graphqlQuery("/pdl/statsborgerskap-uten-historikk.graphql")
        val hentBostedsadresseUtenlandskQuery = graphqlQuery("/pdl/bostedsadresse-utenlandsk.graphql")

        private fun graphqlQuery(path: String) =
            PdlConfig::class.java.getResource(path)!!
                .readText().graphqlCompatible()

        private fun String.graphqlCompatible(): String {
            return StringUtils.normalizeSpace(this.replace("\n", ""))
        }

        fun httpHeaders(): HttpHeaders {
            return HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                accept = listOf(MediaType.APPLICATION_JSON)
                add("Tema", Tema.KON.name)
                add("behandlingsnummer", Tema.KON.behandlingsnummer)
            }
        }
    }
}

enum class PersonInfoQuery(val query: String) {
    ENKEL(PdlConfig.hentEnkelPersonQuery),
    MED_RELASJONER_OG_REGISTERINFORMASJON(PdlConfig.hentPersonMedRelasjonOgRegisterInformasjonQuery),
    NAVN_OG_ADRESSE(PdlConfig.hentPersonMedNavnOgAdresseQuery),
}
