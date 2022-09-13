package no.nav.familie.ks.sak.config

import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class PdlConfig(@Value("\${PDL_URL}") pdlUrl: URI) {

    val pdlUri: URI = UriComponentsBuilder.fromUri(pdlUrl).pathSegment(PATH_GRAPHQL).build().toUri()

    companion object {

        const val PATH_GRAPHQL = "graphql"

        val hentIdenterQuery = graphqlQuery("/pdl/hentIdenter.graphql")

        private fun graphqlQuery(path: String) = PdlConfig::class.java.getResource(path)!!
            .readText().graphqlCompatible()

        private fun String.graphqlCompatible(): String {
            return StringUtils.normalizeSpace(this.replace("\n", ""))
        }

        fun httpHeaders(): HttpHeaders {
            return HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                accept = listOf(MediaType.APPLICATION_JSON)
                add("Tema", "KON")
            }
        }
    }
}
