package no.nav.familie.ks.sak.integrasjon.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.common.exception.PdlNotFoundException
import no.nav.familie.ks.sak.config.PdlConfig
import no.nav.familie.ks.sak.config.PersonInfoQuery
import no.nav.familie.ks.sak.data.randomAktør
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import org.hamcrest.CoreMatchers.`is` as Is

internal class PdlKlientTest {
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var pdlKlient: PdlKlient
    private lateinit var wiremockServerItem: WireMockServer

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        pdlKlient = PdlKlient(PdlConfig(URI.create(wiremockServerItem.baseUrl())), restOperations)
    }

    @Test
    fun `hentPerson skal hente enkel persondata fra PDL med ENKEL query`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(WireMock.okJson(readFile("pdlOkResponseEnkel.json"))),
        )

        val randomAktør = randomAktør()

        val pdlPersonData = pdlKlient.hentPerson(randomAktør, PersonInfoQuery.ENKEL)

        assertThat(pdlPersonData.navn.single().fulltNavn(), Is("ENGASJERT FYR"))
        assertThat(pdlPersonData.kjoenn.single().kjoenn, Is(KJOENN.MANN))
    }

    @Test
    fun `hentPerson skal kaste exception når person ikke eksisterer i PDL`() {
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                .willReturn(WireMock.okJson(readFile("pdlPersonIkkeFunnetResponse.json"))),
        )

        val randomAktør = randomAktør()

        val feil =
            assertThrows<PdlNotFoundException> {
                pdlKlient.hentPerson(randomAktør, PersonInfoQuery.ENKEL)
            }

        assertThat(
            feil.melding,
            Is("Fant ikke person"),
        )
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/pdl/json/$filnavn").readText()
}
