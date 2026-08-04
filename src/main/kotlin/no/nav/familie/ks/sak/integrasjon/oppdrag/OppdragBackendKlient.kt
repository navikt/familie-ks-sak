package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

// Går mot familie-oppdrag-backend som kjører i GCP.
@Service
class OppdragBackendKlient(
    @Value("\${FAMILIE_OPPDRAG_BACKEND_API_URL}")
    private val familieOppdragBackendUri: String,
    @Qualifier("oppdragBackendRestClient") private val restClient: RestClient,
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val uri = URI.create("$familieOppdragBackendUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag-backend",
            uri = uri,
            formål = "Henter simulering fra familie-oppdrag-backend",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(utbetalingsoppdrag)
                .retrieve()
                .body()!!
        }
    }
}
