package no.nav.familie.ks.sak.kjerne.brev

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagNasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

class BrevKlientTest {
    private lateinit var wiremockServer: WireMockServer
    private lateinit var brevKlient: BrevKlient

    @BeforeEach
    fun setUp() {
        wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServer.start()
        brevKlient =
            BrevKlient(
                wiremockServer.baseUrl(),
                "dataset",
                RestClient.builder().build(),
            )
    }

    @AfterEach
    fun tearDown() {
        wiremockServer.stop()
    }

    @Nested
    inner class GenererBrevTest {
        @Test
        fun `skal kaste funksjonell feil ved bad request exception`() {
            val brevDto = mockk<BrevDto>()
            val brevMal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK
            val brevDataDto = mockk<BrevDataDto>()

            every { brevDto.mal } returns brevMal
            every { brevDto.data } returns brevDataDto
            every { brevDataDto.toBrevString() } returns "test"

            wiremockServer.stubFor(
                WireMock
                    .post(WireMock.urlEqualTo("/api/dataset/dokument/NB/${brevMal.apiNavn}/pdf"))
                    .willReturn(WireMock.aResponse().withStatus(400).withBody("msg")),
            )

            val exception =
                assertThrows<FunksjonellFeil> {
                    brevKlient.genererBrev("NB", brevDto)
                }
            assertThat(exception.message).isEqualTo(
                "Det oppsto en feil ved generering av brev. Sjekk at begrunnelsene som er valgt er riktige og kontakt brukerstøtte hvis problemet vedvarer.",
            )
        }

        @Test
        fun `skal ikke konvertere andre exceptions enn bad request exception til funksjonell feil`() {
            val brevDto = mockk<BrevDto>()
            val brevMal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK
            val brevDataDto = mockk<BrevDataDto>()

            every { brevDto.mal } returns brevMal
            every { brevDto.data } returns brevDataDto
            every { brevDataDto.toBrevString() } returns "test"

            wiremockServer.stubFor(
                WireMock
                    .post(WireMock.urlEqualTo("/api/dataset/dokument/NB/${brevMal.apiNavn}/pdf"))
                    .willReturn(WireMock.aResponse().withStatus(403).withBody("msg")),
            )

            assertThrows<HttpClientErrorException.Forbidden> {
                brevKlient.genererBrev("NB", brevDto)
            }
        }
    }

    @Nested
    inner class HentBegrunnelsestekstTest {
        @Test
        fun `skal hente begrunnelsestekst`() {
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            wiremockServer.stubFor(
                WireMock
                    .post(WireMock.urlEqualTo("/ks-sak/begrunnelser/${nasjonalOgFellesBegrunnelseDataDto.apiNavn}/tekst/"))
                    .willReturn(WireMock.ok("bla bla bla")),
            )

            val begrunnelsestekst = brevKlient.hentBegrunnelsestekst(nasjonalOgFellesBegrunnelseDataDto)

            assertThat(begrunnelsestekst).isEqualTo("bla bla bla")
        }

        @Test
        fun `skal kaste funksjonell feil ved bad request exception`() {
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            wiremockServer.stubFor(
                WireMock
                    .post(WireMock.urlEqualTo("/ks-sak/begrunnelser/${nasjonalOgFellesBegrunnelseDataDto.apiNavn}/tekst/"))
                    .willReturn(WireMock.aResponse().withStatus(400).withBody("msg")),
            )

            val exception =
                assertThrows<FunksjonellFeil> {
                    brevKlient.hentBegrunnelsestekst(nasjonalOgFellesBegrunnelseDataDto)
                }
            assertThat(exception.message).isEqualTo(
                "Begrunnelsen '${nasjonalOgFellesBegrunnelseDataDto.apiNavn}' passer ikke vedtaksperioden. Hvis du mener dette er feil ta kontakt med team BAKS.",
            )
        }

        @Test
        fun `skal ikke konvertere andre exceptions enn bad request exception til funksjonell feil`() {
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            wiremockServer.stubFor(
                WireMock
                    .post(WireMock.urlEqualTo("/ks-sak/begrunnelser/${nasjonalOgFellesBegrunnelseDataDto.apiNavn}/tekst/"))
                    .willReturn(WireMock.aResponse().withStatus(403).withBody("msg")),
            )

            assertThrows<HttpClientErrorException.Forbidden> {
                brevKlient.hentBegrunnelsestekst(nasjonalOgFellesBegrunnelseDataDto)
            }
        }
    }
}
