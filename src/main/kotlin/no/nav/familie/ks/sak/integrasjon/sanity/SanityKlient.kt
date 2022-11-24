package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelserResponsDto
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityEØSBegrunnelserResponsDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.net.URLEncoder

@Service
class SanityKlient(
    @Value("\${SANITY_BASE_URL}") private val sanityBaseUrl: String,
    restOperations: RestOperations
) :
    AbstractRestClient(restOperations, "sanity") {
    fun hentBegrunnelser(datasett: String = "ks-test"): List<SanityBegrunnelse> {
        val uri = lagHentUri(datasett, hentBegrunnelser)

        val restSanityBegrunnelser =
            kallEksternTjeneste<SanityBegrunnelserResponsDto>(
                tjeneste = "Sanity",
                uri = uri,
                formål = "Henter begrunnelser fra sanity"
            ) {
                getForEntity(uri)
            }

        return restSanityBegrunnelser.result.map { it.tilSanityBegrunnelse() }
    }

    fun hentEØSBegrunnelser(datasett: String = "ks-test"): List<SanityEØSBegrunnelse> {
        val uri = lagHentUri(datasett, hentEØSBegrunnelser)

        val restSanityEØSBegrunnelser = kallEksternTjeneste<SanityEØSBegrunnelserResponsDto>(
            tjeneste = "Sanity",
            uri = uri,
            formål = "Henter EØS-begrunnelser fra sanity"
        ) {
            getForEntity(uri)
        }

        return restSanityEØSBegrunnelser.result.map { it.tilSanityEØSBegrunnelse() }
    }

    private fun lagHentUri(datasett: String, query: String): URI {
        val hentQuery = URLEncoder.encode(query, "utf-8")
        return URI.create("$sanityBaseUrl/$datasett?query=$hentQuery")
    }
}
