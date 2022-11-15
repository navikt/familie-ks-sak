package no.nav.familie.ks.sak.integrasjon.oppdrag

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequestV2
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient.Companion.RETRY_BACKOFF_5000MS
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class OppdragKlient(
    @Value("\${FAMILIE_OPPDRAG_API_URL}")
    private val familieOppdragUri: String,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "økonomi_kontantstøtte") {

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        val uri = URI.create("$familieOppdragUri/oppdrag")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Iverksetter mot oppdrag"
        ) {
            postForEntity(uri = uri, utbetalingsoppdrag)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat {
        return DetaljertSimuleringResultat(simuleringMottakerMock)
        val uri = URI.create("$familieOppdragUri/simulering/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter simulering fra oppdrag"
        ) {
            postForEntity(uri = uri, utbetalingsoppdrag)
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        val uri = URI.create("$familieOppdragUri/status")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Henter oppdragstatus fra oppdrag"
        ) {
            postForEntity(uri = uri, oppdragId)
        }
    }

    fun grensesnittavstemOppdrag(fraDato: LocalDateTime, tilDato: LocalDateTime): String {
        val uri = URI.create("$familieOppdragUri/grensesnittavstemming")
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Gjør grensesnittavstemming mot oppdrag"
        ) {
            postForEntity(
                uri = uri,
                GrensesnittavstemmingRequest(
                    fagsystem = FAGSYSTEM,
                    fra = fraDato,
                    til = tilDato
                )
            )
        }
    }

    fun konsistensavstemOppdragStart(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID
    ): String {
        val uri = URI.create(
            "$familieOppdragUri/v2/konsistensavstemming" +
                "?sendStartmelding=true&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId"
        )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Start konsistensavstemming mot oppdrag i batch"
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList()
                )
            )
        }
    }

    fun konsistensavstemOppdragData(
        avstemmingsdato: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID
    ): String {
        val uri = URI.create(
            "$familieOppdragUri/v2/konsistensavstemming" +
                "?sendStartmelding=false&sendAvsluttmelding=false&transaksjonsId=$transaksjonsId"
        )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Konsistenstavstemmer chunk mot oppdrag"
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = perioderTilAvstemming
                )
            )
        }
    }

    fun konsistensavstemOppdragAvslutt(
        avstemmingsdato: LocalDateTime,
        transaksjonsId: UUID
    ): String {
        val uri = URI.create(
            "$familieOppdragUri/v2/konsistensavstemming" +
                "?sendStartmelding=false&sendAvsluttmelding=true&transaksjonsId=$transaksjonsId"
        )
        return kallEksternTjenesteRessurs(
            tjeneste = "familie-oppdrag",
            uri = uri,
            formål = "Avslutt konsistensavstemming mot oppdrag"
        ) {
            postForEntity(
                uri = uri,
                KonsistensavstemmingRequestV2(
                    fagsystem = FAGSYSTEM,
                    avstemmingstidspunkt = avstemmingsdato,
                    perioderForBehandlinger = emptyList()
                )
            )
        }
    }

    companion object {
        private const val FAGSYSTEM = "KS"
    }
}

val simulertPosteringMock = listOf(
    SimulertPostering(
        fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
        fom = LocalDate.now().minusMonths(3).førsteDagIInneværendeMåned(),
        tom = LocalDate.now().minusMonths(3).sisteDagIMåned(),
        betalingType = BetalingType.DEBIT,
        beløp = 7500.0.toBigDecimal(),
        posteringType = PosteringType.YTELSE,
        forfallsdato = LocalDate.now(),
        utenInntrekk = false
    ),
    SimulertPostering(
        fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
        fom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
        tom = LocalDate.now().minusMonths(2).sisteDagIMåned(),
        betalingType = BetalingType.DEBIT,
        beløp = 6500.0.toBigDecimal(),
        posteringType = PosteringType.YTELSE,
        forfallsdato = LocalDate.now().plusMonths(1),
        utenInntrekk = false
    ),
    SimulertPostering(
        fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
        fom = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
        tom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
        betalingType = BetalingType.DEBIT,
        beløp = 5000.0.toBigDecimal(),
        posteringType = PosteringType.YTELSE,
        forfallsdato = LocalDate.now().plusMonths(2),
        utenInntrekk = false
    )
)

val simuleringMottakerMock = listOf(
    SimuleringMottaker(
        simulertPostering = simulertPosteringMock,
        mottakerType = MottakerType.BRUKER,
        mottakerNummer = "12345678910"
    )
)
