package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.retryVedException
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelserResponsDto
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.net.URLEncoder

@Service
class SanityKlient(
    @Value("\${SANITY_BASE_URL}") private val sanityBaseUrl: String,
    restOperations: RestOperations,
    @Value("$RETRY_BACKOFF_5000MS") private val retryBackoffDelay: Long,
) : AbstractRestClient(restOperations, "sanity") {
    fun hentBegrunnelser(datasett: String = "ks-brev"): List<SanityBegrunnelse> {
        val uri = lagHentUri(datasett, HENT_BEGRUNNELSER)

        val restSanityBegrunnelser =
            kallEksternTjeneste<SanityBegrunnelserResponsDto>(
                tjeneste = "Sanity",
                uri = uri,
                formål = "Henter begrunnelser fra sanity",
            ) {
                retryVedException(retryBackoffDelay).execute {
                    getForEntity(uri)
                }
            }

        return restSanityBegrunnelser.result.map { it.tilSanityBegrunnelse() }
    }

    private fun lagHentUri(
        datasett: String,
        query: String,
    ): URI {
        val hentQuery = URLEncoder.encode(query, "utf-8")
        return URI.create("$sanityBaseUrl/$datasett?query=$hentQuery")
    }
}
