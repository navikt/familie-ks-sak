package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagNasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.BrevDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

class BrevKlientTest {
    private val mockedRestClient: RestClient = mockk()
    private val baseUri = "http://localhost:8080"
    private val brevKlient: BrevKlient =
        BrevKlient(
            baseUri,
            "dataset",
            mockedRestClient,
        )

    @Nested
    inner class GenererBrevTest {
        @Test
        fun `skal kaste funksjonell feil ved bad request exception`() {
            // Arrange
            val brevDto = mockk<BrevDto>()
            val brevMal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK
            val brevDataDto = mockk<BrevDataDto>()

            every { brevDto.mal } returns brevMal
            every { brevDto.data } returns brevDataDto
            every { brevDataDto.toBrevString() } returns "test"

            every {
                mockedRestClient.post()
            } throws
                HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "text",
                    HttpHeaders.EMPTY,
                    "msg".toByteArray(),
                    null,
                )

            // Act & assert
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
            // Arrange
            val brevDto = mockk<BrevDto>()
            val brevMal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK
            val brevDataDto = mockk<BrevDataDto>()

            every { brevDto.mal } returns brevMal
            every { brevDto.data } returns brevDataDto
            every { brevDataDto.toBrevString() } returns "test"

            every {
                mockedRestClient.post()
            } throws
                HttpClientErrorException.create(
                    HttpStatus.FORBIDDEN,
                    "text",
                    HttpHeaders.EMPTY,
                    "msg".toByteArray(),
                    null,
                )

            // Act & assert
            assertThrows<HttpClientErrorException.Forbidden> {
                brevKlient.genererBrev("NB", brevDto)
            }
        }
    }

    @Nested
    inner class HentBegrunnelsestekstTest {
        @Test
        fun `skal hente begrunnelsestekst`() {
            // Arrange
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            val requestBodyUriSpec = mockk<RestClient.RequestBodyUriSpec>()
            val requestBodySpec = mockk<RestClient.RequestBodySpec>()
            val responseSpec = mockk<RestClient.ResponseSpec>()

            every { mockedRestClient.post() } returns requestBodyUriSpec
            every { requestBodyUriSpec.uri(any<URI>()) } returns requestBodySpec
            every { requestBodySpec.body(any<Any>()) } returns requestBodySpec
            every { requestBodySpec.retrieve() } returns responseSpec
            every { responseSpec.body<String>() } returns "bla bla bla"

            // Act
            val begrunnelsestekst = brevKlient.hentBegrunnelsestekst(nasjonalOgFellesBegrunnelseDataDto)

            // Assert
            assertThat(begrunnelsestekst).isEqualTo("bla bla bla")
        }

        @Test
        fun `skal kaste funksjonell feil ved bad request exception`() {
            // Arrange
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            every {
                mockedRestClient.post()
            } throws
                HttpClientErrorException.create(
                    HttpStatus.BAD_REQUEST,
                    "text",
                    HttpHeaders.EMPTY,
                    "msg".toByteArray(),
                    null,
                )

            // Act & assert
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
            // Arrange
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            every {
                mockedRestClient.post()
            } throws
                HttpClientErrorException.create(
                    HttpStatus.FORBIDDEN,
                    "text",
                    HttpHeaders.EMPTY,
                    "msg".toByteArray(),
                    null,
                )

            // Act & assert
            val exception =
                assertThrows<HttpClientErrorException.Forbidden> {
                    brevKlient.hentBegrunnelsestekst(nasjonalOgFellesBegrunnelseDataDto)
                }
            assertThat(exception.message).isEqualTo("403 text")
        }
    }
}
