package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

private const val FAMILIE_OPPDRAG = "familie-oppdrag"

@Service
class AvstemmingKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("jwtBearerLongTimeout") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "økonomi_kontantstøtte") {
    fun sendGrensesnittavstemmingTilOppdrag(
        fom: LocalDateTime,
        tom: LocalDateTime,
        avstemmingId: UUID?,
    ): String {
        val uri = URI.create("$familieOppdragUri/grensesnittavstemming")
        return kallEksternTjenesteRessurs(
            tjeneste = FAMILIE_OPPDRAG,
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
            tjeneste = FAMILIE_OPPDRAG,
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
            tjeneste = FAMILIE_OPPDRAG,
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
            tjeneste = FAMILIE_OPPDRAG,
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
            tjeneste = FAMILIE_OPPDRAG,
            uri = uri,
            formål = "sov",
        ) {
            getForEntity(
                uri = uri,
            )
        }
    }
}
