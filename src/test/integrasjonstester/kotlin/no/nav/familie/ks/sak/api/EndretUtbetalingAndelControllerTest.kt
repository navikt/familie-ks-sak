package no.nav.familie.ks.sak.api

import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class EndretUtbetalingAndelControllerTest : OppslagSpringRunnerTest() {
    private val controllerUrl: String = "/api/endretutbetalingandel"

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class OppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelseTest {
        @Test
        fun `skal returnere UNAUTHORIZED om token ikke er satt`() {
            // Act & assert
            When {
                put("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.UNAUTHORIZED.value())
            }
        }

        @Test
        fun `skal oppdatere EndretUtbetalingAndel`() {
            // Arrange
            val søker = opprettOgLagreSøker()
            val fagsak = opprettOgLagreFagsak(lagFagsak(aktør = søker))
            val behandling = opprettOgLagreBehandling(lagBehandling(fagsak = fagsak))

            val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

            // Act & assert
            Given {
                header("Authorization", "Bearer $token")
            } When {
                put("$controllerUrl/${behandling.id}")
            } Then {
                statusCode(HttpStatus.OK.value())
                /*
                body("data.behandlingId", `is`(behandling.id.toInt()))
                body("status", `is`("SUKSESS"))
                body("melding", `is`("Innhenting av data var vellykket"))
                body("frontendFeilmelding", nullValue())
                 */
            }
        }
    }
}
