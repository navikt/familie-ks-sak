package no.nav.familie.ks.sak.integrasjon.nbr

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.ks.sak.common.exception.IntegrasjonException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestClient

class NasjonalBarnehageRegisterKlientTest {
    private lateinit var wiremockServer: WireMockServer
    private lateinit var nasjonalBarnehageRegisterKlient: NasjonalBarnehageRegisterKlient

    @BeforeEach
    fun setUp() {
        wiremockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServer.start()
        nasjonalBarnehageRegisterKlient = NasjonalBarnehageRegisterKlient(wiremockServer.baseUrl(), RestClient.builder().build())
    }

    @AfterEach
    fun tearDown() {
        wiremockServer.stop()
    }

    @Test
    fun `hentEnhet skal mappe enhet og foreldrerelasjoner fra NBR`() {
        // Arrange
        wiremockServer.stubFor(
            WireMock
                .get(urlEqualTo("/v4/enhet/973906887"))
                .willReturn(
                    okJson(
                        """
                        {
                          "Organisasjonsnummer": "973906887",
                          "Navn": "Hvitveisen barnehage AS",
                          "ErAktiv": true,
                          "ErBarnehage": true,
                          "Koordinat": { "Lengdegrad": 10.7, "Breddegrad": 59.9 },
                          "ForeldreRelasjoner": [
                            {
                              "Enhet": { "Organisasjonsnummer": "970145311", "Navn": "Oslo kommune bydel 7 Vestre Aker" },
                              "Relasjonstype": { "Id": "10", "Navn": "Administrativ struktur" }
                            },
                            {
                              "Enhet": { "Organisasjonsnummer": "982791502", "Navn": "Hvitveisen barnehage AS" },
                              "Relasjonstype": { "Id": "6", "Navn": "Eierstruktur" }
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        // Act
        val enhet = nasjonalBarnehageRegisterKlient.hentEnhet("973906887")

        // Assert
        assertThat(enhet).isNotNull
        assertThat(enhet!!.organisasjonsnummer).isEqualTo("973906887")
        assertThat(enhet.navn).isEqualTo("Hvitveisen barnehage AS")
        assertThat(enhet.erAktiv).isTrue()
        assertThat(
            enhet.foreldreRelasjoner!!
                .first()
                .relasjonstype.navn,
        ).isEqualTo("Administrativ struktur")
        assertThat(enhet.foreldreRelasjoner!!.map { it.enhet.organisasjonsnummer }).containsExactlyInAnyOrder("970145311", "982791502")
    }

    @Test
    fun `hentEnhet skal returnere null når enheten ikke finnes i NBR`() {
        // Arrange
        wiremockServer.stubFor(
            WireMock
                .get(urlEqualTo("/v4/enhet/123456789"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("""{"title":"Not Found","status":404}"""),
                ),
        )

        // Act
        val enhet = nasjonalBarnehageRegisterKlient.hentEnhet("123456789")

        // Assert
        assertThat(enhet).isNull()
    }

    @Test
    fun `hentEnhet skal kaste IntegrasjonException når NBR svarer med serverfeil`() {
        // Arrange
        wiremockServer.stubFor(
            WireMock
                .get(urlEqualTo("/v4/enhet/973906887"))
                .willReturn(aResponse().withStatus(503)),
        )

        // Act & Assert
        assertThrows<IntegrasjonException> {
            nasjonalBarnehageRegisterKlient.hentEnhet("973906887")
        }
    }
}
