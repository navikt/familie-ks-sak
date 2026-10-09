package no.nav.familie.ks.sak.api

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.datagenerator.BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN
import no.nav.familie.ks.sak.datagenerator.lagPersonTilgangAvvistGrunnetSkjerming
import no.nav.familie.ks.sak.fake.FakeFeatureToggleService
import no.nav.familie.ks.sak.fake.FakeIntegrasjonKlient
import no.nav.familie.ks.sak.fake.FakeTilgangsmaskinTilgangskontrollKlient
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.core.StringContains
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AInntektControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fakeTilgangsmaskinTilgangskontrollKlient: FakeTilgangsmaskinTilgangskontrollKlient

    @Autowired
    private lateinit var fakeIntegrasjonKlient: FakeIntegrasjonKlient

    @Autowired
    private lateinit var fakeFeatureToggleService: FakeFeatureToggleService

    private val apiUrl = "/api/a-inntekt/hent-url"
    private val ident = "01012012345"
    private var token = ""
    private val ainntektUrl = "/test/1234"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
    }

    @AfterEach
    fun tearDown() {
        fakeTilgangsmaskinTilgangskontrollKlient.reset()
        fakeIntegrasjonKlient.reset()
        fakeFeatureToggleService.reset()
    }

    @Test
    fun `kan hente ut A-Inntekt url`() {
        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(jsonMapper.writeValueAsString(PersonIdent(ident)))
            // Act & Assert
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
        // Arrange
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming(ident)))

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(jsonMapper.writeValueAsString(PersonIdent(ident)))
            // Act & Assert
        } When {
            post(apiUrl)
        } Then {
            statusCode(403)
            body("status", `is`("IKKE_TILGANG"))
            body("melding", StringContains("Saksbehandler test har ikke tilgang til å behandle alle personene som etterspørres. $BEGRUNNELSE_SKJERMING_FRA_TILGANGSMASKINEN."))
            body("data", `is`(nullValue()))
        }
    }

    @Test
    fun `kan hente ut A-Inntekt url når familie-integrasjoner gir tilgang og togglen for Tilgangsmaskinen er av`() {
        // Arrange
        fakeFeatureToggleService.set(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN, false)
        fakeTilgangsmaskinTilgangskontrollKlient.leggTilTilganger(listOf(lagPersonTilgangAvvistGrunnetSkjerming(ident)))

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(jsonMapper.writeValueAsString(PersonIdent(ident)))
            // Act & Assert
        } When {
            post(apiUrl)
        } Then {
            statusCode(200)
            body("status", `is`("SUKSESS"))
            body("data", `is`(ainntektUrl))
        }
    }

    @Test
    fun `har ikke tilgang til å hente ut A-Inntekt url når familie-integrasjoner avviser og togglen for Tilgangsmaskinen er av`() {
        // Arrange
        fakeFeatureToggleService.set(FeatureToggle.SKAL_BRUKE_TILGANGSMASKINEN, false)
        fakeIntegrasjonKlient.leggTilTilganger(listOf(Tilgang(personIdent = ident, harTilgang = false)))

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(jsonMapper.writeValueAsString(PersonIdent(ident)))
            // Act & Assert
        } When {
            post(apiUrl)
        } Then {
            statusCode(403)
            body("status", `is`("IKKE_TILGANG"))
            body("melding", StringContains("Saksbehandler test har ikke tilgang til å behandle alle personene som etterspørres. familie-integrasjoner ga ikke tilgang til personen."))
            body("data", `is`(nullValue()))
        }
    }
}
