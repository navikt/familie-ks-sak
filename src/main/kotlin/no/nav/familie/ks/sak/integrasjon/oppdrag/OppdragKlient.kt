package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.retryVedException
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class OppdragKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearer") restOperations: RestOperations,
    @Value("$RETRY_BACKOFF_5000MS") private val retryBackoffDelay: Long,
) : AbstractRestClient(restOperations, "økonomi_kontantstøtte") {
    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        val uri = URI.create("$familieOppdragUri/oppdrag")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Iverksetter mot oppdrag",
        ) {
            postForEntity(uri = uri, utbetalingsoppdrag)
        }
    }

    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        val uri = URI.create("$familieOppdragUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter simulering fra oppdrag",
        ) {
            retryVedException(retryBackoffDelay).execute {
                postForEntity(uri = uri, utbetalingsoppdrag)
            }
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter oppdragstatus fra oppdrag",
        ) {
            postForEntity(uri = uri, oppdragId)
        }
    }

    fun hentSisteUtbetalingsoppdragForFagsaker(fagsakIder: Set<Long>): List<UtbetalingsoppdragMedBehandlingOgFagsak> {
        val uri = URI.create("$familieOppdragUri/$FAGSYSTEM/fagsaker/siste-utbetalingsoppdrag")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Hent utbetalingsoppdrag for fagsaker",
        ) { postForEntity(uri = uri, payload = fagsakIder) }
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
