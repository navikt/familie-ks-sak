package no.nav.familie.ks.sak.integrasjon.oppdrag

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

internal class OppdragKlientTest {
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var oppdragKlient: OppdragKlient
    private lateinit var wiremockServerItem: WireMockServer

    @BeforeEach
    fun beforeEach() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        oppdragKlient = OppdragKlient(wiremockServerItem.baseUrl(), restOperations, 1)
    }

    @Test
    fun `hentSimulering - skal hente simulering for utbetalingsoppdrag`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/simulering/v1"))
                .willReturn(WireMock.okJson(readFile("hentSimulering.json"))),
        )

        val simulering =
            oppdragKlient.hentSimulering(
                Utbetalingsoppdrag(
                    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                    fagSystem = "KS",
                    saksnummer = "1234",
                    aktoer = "12345678910",
                    saksbehandlerId = "Z12345",
                    avstemmingTidspunkt = LocalDateTime.now(),
                    utbetalingsperiode =
                        listOf(
                            Utbetalingsperiode(
                                erEndringPåEksisterendePeriode = false,
                                periodeId = 1,
                                datoForVedtak = LocalDate.now(),
                                klassifisering = "klassifisering",
                                vedtakdatoFom = LocalDate.of(2021, 1, 1),
                                vedtakdatoTom = LocalDate.of(2021, 5, 31),
                                sats = BigDecimal.valueOf(7500),
                                satsType = Utbetalingsperiode.SatsType.MND,
                                utbetalesTil = "12345678910",
                                behandlingId = 1,
                            ),
                        ),
                ),
            )

        assertThat(simulering.simuleringMottaker.size, Is(1))
    }

    @Test
    fun `hentStatus - skal hente status for oppdragId`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/status"))
                .willReturn(WireMock.okJson(readFile("hentStatus.json"))),
        )

        val oppdragStatus = oppdragKlient.hentStatus(OppdragId("KS", "12345678910", "1"))

        assertThat(oppdragStatus, Is(OppdragStatus.KVITTERT_OK))
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/oppdrag/json/$filnavn").readText()
}
