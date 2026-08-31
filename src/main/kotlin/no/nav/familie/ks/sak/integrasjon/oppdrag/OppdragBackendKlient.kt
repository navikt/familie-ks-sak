package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
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
    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        val uri = URI.create("$familieOppdragBackendUri/oppdrag")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Iverksetter mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(utbetalingsoppdrag)
                .retrieve()
                .body()!!
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val uri = URI.create("$familieOppdragBackendUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
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

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragBackendUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Henter oppdragstatus fra oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(oppdragId)
                .retrieve()
                .body()!!
        }
    }

    fun hentSisteUtbetalingsoppdragForFagsaker(
        fagsakIder: Set<Long>,
    ): List<UtbetalingsoppdragMedBehandlingOgFagsak> {
        val uri = URI.create("$familieOppdragBackendUri/$FAGSYSTEM/fagsaker/siste-utbetalingsoppdrag")

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Hent utbetalingsoppdrag for fagsaker",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(fagsakIder)
                .retrieve()
                .body()!!
        }
    }

    companion object {
        private const val FAMILIE_OPPDRAG_BACKEND = "familie-oppdrag-backend"
    }
}
