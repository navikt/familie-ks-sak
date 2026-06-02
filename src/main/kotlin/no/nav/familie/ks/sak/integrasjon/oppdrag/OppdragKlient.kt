package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
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

@Service
class OppdragKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("oppdragRestClient") private val restClient: RestClient,
) {
    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        val uri = URI.create("$familieOppdragUri/oppdrag")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
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
        val uri = URI.create("$familieOppdragUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter simulering fra oppdrag",
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
        val uri = URI.create("$familieOppdragUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
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

    fun hentSisteUtbetalingsoppdragForFagsaker(fagsakIder: Set<Long>): List<UtbetalingsoppdragMedBehandlingOgFagsak> {
        val uri = URI.create("$familieOppdragUri/$FAGSYSTEM/fagsaker/siste-utbetalingsoppdrag")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
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
        private const val FAGSYSTEM = "KS"
    }
}

data class UtbetalingsoppdragMedBehandlingOgFagsak(
    val fagsakId: Long,
    val behandlingId: Long,
    val utbetalingsoppdrag: Utbetalingsoppdrag,
)
