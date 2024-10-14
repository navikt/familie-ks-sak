package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagNasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalOgFellesBegrunnelseDataDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI

class BrevKlientTest {
    private val mockedRestOperations: RestOperations = mockk()
    private val baseUri = "http://localhost:8080"
    private val brevKlient: BrevKlient =
        BrevKlient(
            baseUri,
            "dataset",
            mockedRestOperations,
        )

    @Nested
    inner class HentBegrunnelsestekstTest {
        @Test
        fun `skal hente begrunnelsestekst`() {
            // Arrange
            val nasjonalOgFellesBegrunnelseDataDto = lagNasjonalOgFellesBegrunnelseDataDto()

            every {
                mockedRestOperations.exchange<String>(
                    eq(URI("$baseUri/ks-sak/begrunnelser/${nasjonalOgFellesBegrunnelseDataDto.apiNavn}/tekst/")),
                    eq(HttpMethod.POST),
                    any<HttpEntity<NasjonalOgFellesBegrunnelseDataDto>>(),
                )
            } returns
                ResponseEntity<String>(
                    "bla bla bla",
                    HttpStatus.OK,
                )

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
                mockedRestOperations.exchange<String>(
                    eq(URI("$baseUri/ks-sak/begrunnelser/${nasjonalOgFellesBegrunnelseDataDto.apiNavn}/tekst/")),
                    eq(HttpMethod.POST),
                    any<HttpEntity<NasjonalOgFellesBegrunnelseDataDto>>(),
                )
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
                mockedRestOperations.exchange<String>(
                    eq(URI("$baseUri/ks-sak/begrunnelser/${nasjonalOgFellesBegrunnelseDataDto.apiNavn}/tekst/")),
                    eq(HttpMethod.POST),
                    any<HttpEntity<NasjonalOgFellesBegrunnelseDataDto>>(),
                )
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
