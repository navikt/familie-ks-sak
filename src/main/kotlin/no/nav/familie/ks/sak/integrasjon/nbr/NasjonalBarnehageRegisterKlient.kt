package no.nav.familie.ks.sak.integrasjon.nbr

import no.nav.familie.ks.sak.common.exception.IntegrasjonException
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.nbr.domene.NbrEnhet
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder

/**
 * Klient mot Nasjonalt barnehageregister (NBR) hos Utdanningsdirektoratet.
 * API-et er åpent og krever ikke autentisering.
 */
@Service
class NasjonalBarnehageRegisterKlient(
    @Value("\${NBR_URL}") private val nbrUrl: String,
    @Qualifier("utenAuthRestClient") private val restClient: RestClient,
) {
    /**
     * Returnerer enheten med foreldrerelasjoner, eller null hvis organisasjonsnummeret ikke finnes i NBR (404).
     *
     * 404 gir null i stedet for feil fordi et nytt forsøk ikke vil gi et annet svar, f.eks. når barnehagelisten
     * inneholder et organisasjonsnummer som ikke er registrert i NBR. Andre 4xx-feil kastes videre uten nye forsøk,
     * mens øvrige feil kastes som [IntegrasjonException] og forsøkes på nytt.
     */
    @Retryable(
        value = [IntegrasjonException::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    @Cacheable("nasjonalBarnehageRegister", cacheManager = "shortCache")
    fun hentEnhet(barnehageOrganisasjonsnummer: String): NbrEnhet? {
        val uri =
            UriComponentsBuilder
                .fromUriString(nbrUrl)
                .pathSegment("v4", "enhet", barnehageOrganisasjonsnummer)
                .build()
                .toUri()

        return try {
            kallEksternTjeneste<NbrEnhet>(
                tjeneste = "NBR",
                uri = uri,
                formål = "Henter enhet fra Nasjonalt barnehageregister",
            ) {
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body<NbrEnhet>()!!
            }
        } catch (_: HttpClientErrorException.NotFound) {
            null
        }
    }
}
