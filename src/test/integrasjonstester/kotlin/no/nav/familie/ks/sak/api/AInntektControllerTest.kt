package no.nav.familie.ks.sak.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.core.StringContains
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AInntektControllerTest : OppslagSpringRunnerTest() {
    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    private val apiUrl = "/api/a-inntekt/hent-url"
    private val ident = "01012012345"
    private var token = ""
    private val ainntektUrl = "test/url"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(ident, true))
        every { integrasjonClient.hentAInntektUrl(any()) } returns ainntektUrl
    }

    @Test
    fun `kan hente ut A-Inntekt url`() {
        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(objectMapper.writeValueAsString(PersonIdent(ident)))
        } When {
            post(apiUrl)
        } Then {
            statusCode(200)
            body("status", `is`("SUKSESS"))
            body("melding", `is`("Innhenting av data var vellykket"))
            body("data", `is`(ainntektUrl))
        }
    }

    @Test
    fun `har ikke tilgang til å hente ut A-Inntekt url`() {
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(ident, false))

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(objectMapper.writeValueAsString(PersonIdent(ident)))
        } When {
            post(apiUrl)
        } Then {
            statusCode(403)
            body("status", `is`("IKKE_TILGANG"))
            body("melding", StringContains("Saksbehandler test har ikke tilgang til å behandle [01012012345]."))
            body("data", `is`(nullValue()))
        }
    }
}
