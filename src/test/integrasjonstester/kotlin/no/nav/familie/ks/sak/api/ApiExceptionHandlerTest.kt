package no.nav.familie.ks.sak.no.nav.familie.ks.sak.api

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.hamcrest.CoreMatchers.`is` as Is

class ApiExceptionHandlerTest : OppslagSpringRunnerTest() {
    private val controllerUrl: String = "/api/forvaltning"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun `Skal få en feilmelding om hvilket felt som ikke kan være null, som er null i json`() {
        // Arrange
        val gyldigEndretUtbetalingAndelDto =
            """
            {
               "fom": null,
               "tom": "2021-01-01"
            }
            """.trimIndent()

        val token = lokalTestToken(behandlerRolle = BehandlerRolle.SAKSBEHANDLER)

        // Act & assert
        Given {
            header("Authorization", "Bearer $token")
            body(gyldigEndretUtbetalingAndelDto)
            contentType(ContentType.JSON)
        } When {
            post("$controllerUrl/avstemming/send-grensesnittavstemming-manuell")
        } Then {
            statusCode(HttpStatus.BAD_REQUEST.value())
            body("status", Is("FEILET"))
            body("frontendFeilmelding", Is("Mangler verdi for felt no.nav.familie.ks.sak.common.util.Periode[\"fom\"]"))
        }
    }

    @Test
    fun `Skal få en feilmelding om hvilket felt som ikke kan parses`() {
        // Arrange
        val gyldigEndretUtbetalingAndelDto =
            """
            {
               "fom": "2021",
               "tom": "2021-01-01"
            }
            """.trimIndent()

        val token = lokalTestToken(behandlerRolle = BehandlerRolle.SAKSBEHANDLER)

        // Act & assert
        Given {
            header("Authorization", "Bearer $token")
            body(gyldigEndretUtbetalingAndelDto)
            contentType(ContentType.JSON)
        } When {
            post("$controllerUrl/avstemming/send-grensesnittavstemming-manuell")
        } Then {
            statusCode(HttpStatus.BAD_REQUEST.value())
            body("status", Is("FEILET"))
            body("frontendFeilmelding", Is("Ugyldig verdi 2021 for felt no.nav.familie.ks.sak.common.util.Periode[\"fom\"]"))
        }
    }
}
