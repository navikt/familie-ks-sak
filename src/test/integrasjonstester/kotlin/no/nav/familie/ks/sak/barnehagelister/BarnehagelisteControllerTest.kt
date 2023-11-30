package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehageListeService
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class BarnehagelisteControllerTest(
    @Autowired private val barnehageListeService: BarnehageListeService,
) : OppslagSpringRunnerTest() {
    @MockK
    lateinit var tilgangService: TilgangService

    @BeforeEach
    fun setup() {
        RestAssured.port = port
    }

    @Test
    fun `hent barnehageliste skal gi 200 OK dersom alt virker som det skal`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

        val barnehagelisteMottatt =
            BarnehagelisteMottatt(
                melding = BarnehagelisteServiceTest.barnehagelisteXml,
                meldingId = "testId",
                mottatTid = LocalDateTime.now(),
            )
        barnehageListeService.lagreBarnehagelisteMottattOgOpprettTaskForLesing(barnehagelisteMottatt)
        Assertions.assertNotNull(barnehageListeService.hentUarkiverteBarnehagelisteUuider())
        barnehageListeService.lesOgArkiver(barnehagelisteMottatt.id)
        every { tilgangService.validerTilgangTilHandling(any(), any()) }

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(objectMapper.writeValueAsString(BarnehagebarnRequestParams(ident = "", kommuneNavn = "", kunLøpendeFagsak = false)))
        } When {
            post("/api/barnehagebarn/barnehagebarnliste")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("data.årsak", CoreMatchers.`is`("SØKNAD"))
        }
    }
}
