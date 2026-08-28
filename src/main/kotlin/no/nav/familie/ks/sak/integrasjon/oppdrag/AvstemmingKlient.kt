package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

@Service
class AvstemmingKlient(
    @Value("\${FAMILIE_OPPDRAG_BACKEND_API_URL}")
    private val familieOppdragBackendUri: String,
    @Qualifier("avstemmingRestClient") private val restClient: RestClient,
) {
    fun sendGrensesnittavstemmingTilOppdrag(
        fom: LocalDateTime,
        tom: LocalDateTime,
        avstemmingId: UUID?,
    ): String {
        val uri = URI.create("$familieOppdragBackendUri/grensesnittavstemming")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Gjør grensesnittavstemming mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    GrensesnittavstemmingRequest(
                        fagsystem = FAGSYSTEM,
                        fra = fom,
                        til = tom,
                        avstemmingId = avstemmingId,
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun konsistensavstemOppdragStart(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/v2/konsistensavstemming" +
                    "?sendStartmelding=true&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Start konsistensavstemming mot oppdrag i batch",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    KonsistensavstemmingRequestV2(
                        fagsystem = FAGSYSTEM,
                        avstemmingstidspunkt = avstemmingsdato,
                        perioderForBehandlinger = emptyList(),
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId",
            )

        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Konsistenstavstemmer chunk mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    KonsistensavstemmingRequestV2(
                        fagsystem = FAGSYSTEM,
                        avstemmingstidspunkt = avstemmingsdato,
                        perioderForBehandlinger = perioderTilAvstemming,
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun konsistensavstemOppdragAvslutt(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/v2/konsistensavstemming" +
                    "?sendStartmelding=false&sendAvsluttmelding=true&transaksjonsId=$transaksjonsId",
            )
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "Avslutt konsistensavstemming mot oppdrag",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(
                    KonsistensavstemmingRequestV2(
                        fagsystem = FAGSYSTEM,
                        avstemmingstidspunkt = avstemmingsdato,
                        perioderForBehandlinger = emptyList(),
                    ),
                ).retrieve()
                .body()!!
        }
    }

    fun sov(
        sovAntallSekunder: Long,
    ): String {
        val uri =
            URI.create(
                "$familieOppdragBackendUri/timeout-test?sekunder=$sovAntallSekunder",
            )
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG_BACKEND,
            uri = uri,
            formål = "sov",
        ) {
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body()!!
        }
    }

    companion object {
        private const val FAMILIE_OPPDRAG_BACKEND = "familie-oppdrag-backend"
    }
}
