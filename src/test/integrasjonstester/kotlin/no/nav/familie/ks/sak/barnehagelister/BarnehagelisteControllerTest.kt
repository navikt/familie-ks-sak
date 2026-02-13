package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehageListeService
import no.nav.familie.ks.sak.barnehagelister.BarnehagelisteServiceTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.config.BehandlerRolle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class BarnehagelisteControllerTest(
    @Autowired private val barnehageListeService: BarnehageListeService,
) : OppslagSpringRunnerTest() {
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
        barnehageListeService.lesOgArkiverBarnehageliste(barnehagelisteMottatt.id)

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(jsonMapper.writeValueAsString(BarnehagebarnRequestParams(ident = "", kommuneNavn = "", kunLøpendeAndel = false)))
        } When {
            post("/api/barnehagebarn/barnehagebarnliste")
        } Then {
            statusCode(HttpStatus.OK.value())
        }
    }
}
