package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@Service
class OppdragKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearer") restOperations: RestOperations,
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
            postForEntity(uri = uri, utbetalingsoppdrag)
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

    fun sendGrensesnittavstemmingTilOppdrag(
        fom: LocalDateTime,
        tom: LocalDateTime,
        avstemmingId: UUID?,
    ): String {
        val uri = URI.create("$familieOppdragUri/grensesnittavstemming")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Gjør grensesnittavstemming mot oppdrag",
        ) {
            postForEntity(
                uri = uri,
                payload =
                    GrensesnittavstemmingRequest(
                        fagsystem = FAGSYSTEM,
                        fra = fom,
                        til = tom,
                        avstemmingId = avstemmingId,
                    ),
            )
        }
    }

    fun konsistensavstemOppdragStart(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/v2/konsistensavstemming" +
                    "?sendStartmelding=true&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Start konsistensavstemming mot oppdrag i batch",
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList(),
                ),
            )
        }
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Konsistenstavstemmer chunk mot oppdrag",
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = perioderTilAvstemming,
                ),
            )
        }
    }

    fun konsistensavstemOppdragAvslutt(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=true&transaksjonsId=$transaksjonsId",
            )
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Avslutt konsistensavstemming mot oppdrag",
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList(),
                ),
            )
        }
    }

    fun sov(
        sovAntallSekunder: Long,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragUri/timeout-test?sekunder=$sovAntallSekunder",
            )
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "sov",
        ) {
            getForEntity(
                uri = uri,
            )
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
