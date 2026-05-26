package no.nav.familie.ks.sak.integrasjon.sanity

import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelserResponsDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.net.URLEncoder

@Service
class SanityKlient(
    @Value("\${SANITY_BASE_URL}") private val sanityBaseUrl: String,
    @Qualifier("utenAuthRestClient") private val restClient: RestClient,
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentBegrunnelser(datasett: String = "ks-brev"): List<SanityBegrunnelse> {
        val uri = lagHentUri(datasett, HENT_BEGRUNNELSER)

        val restSanityBegrunnelser =
            kallEksternTjeneste<SanityBegrunnelserResponsDto>(
                tjeneste = "Sanity",
                uri = uri,
                formål = "Henter begrunnelser fra sanity",
            ) {
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body<SanityBegrunnelserResponsDto>()!!
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
