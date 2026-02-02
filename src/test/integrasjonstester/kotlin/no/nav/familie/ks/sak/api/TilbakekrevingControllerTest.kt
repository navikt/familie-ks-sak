package no.nav.familie.ks.sak.no.nav.familie.ks.sak.api

import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.fake.FakeTilbakekrevingKlient
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.UUID
import org.hamcrest.CoreMatchers.`is` as Is

@Disabled
class TilbakekrevingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fakeTilbakekrevingKlient: FakeTilbakekrevingKlient

    private val controllerUrl = "/api/tilbakekreving"

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        fakeTilbakekrevingKlient.reset()
    }

    @Test
    fun `hentTilbakekrevingsbehandlinger - skal returnere 401 unauthorized dersom brukeren ikke har token for å hente behandling`() {
        When {
            get("$controllerUrl/fagsak/123456")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
            body("status", Is("FEILET"))
        }
    }

    @Test
    fun `hentTilbakekrevingsbehandlinger - skal returnere 403 dersom brukeren ikke har rettigheter til å hente tilbakekrevingsbehandlinger`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.UKJENT)

        Given {
            header("Authorization", "Bearer $token")
        } When {
            get("$controllerUrl/fagsak/123456")
        } Then {
            statusCode(HttpStatus.FORBIDDEN.value())
        }
    }

    @Test
    fun `hentTilbakekrevingsbehandlinger - skal returnere liste med tilbakekrevingsbehandlinger og 200 OK dersom behandlinger er hentet fra familie-tilbake`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.VEILEDER)

        val tilbakekrevingsbehandling =
            Behandling(
                behandlingId = UUID.randomUUID(),
                opprettetTidspunkt = LocalDateTime.now(),
                aktiv = true,
                årsak = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                type = Behandlingstype.TILBAKEKREVING,
                status = Behandlingsstatus.AVSLUTTET,
                vedtaksdato = LocalDateTime.now(),
                resultat = Behandlingsresultatstype.FULL_TILBAKEBETALING,
            )

        fakeTilbakekrevingKlient.returnerteBehandlinger = listOf(tilbakekrevingsbehandling)

        Given {
            header("Authorization", "Bearer $token")
        } When {
            get("$controllerUrl/fagsak/123456")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("status", Is("SUKSESS"))
            body("data", hasSize<Int>(1))
            body("data[0].behandlingId", Is(tilbakekrevingsbehandling.behandlingId.toString()))
            body("data[0].resultat", Is(tilbakekrevingsbehandling.resultat.toString()))
            body("data[0].årsak", Is(tilbakekrevingsbehandling.årsak.toString()))
            body("data[0].type", Is(tilbakekrevingsbehandling.type.toString()))
        }
    }

    @Test
    fun `hentTilbakekrevingsbehandlinger - skal feile dersom kall mot familie-tilbake feiler`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        fakeTilbakekrevingKlient.errorVedHentingAvBehandlinger = true

        Given {
            header("Authorization", "Bearer $token")
        } When {
            get("$controllerUrl/fagsak/123456")
        } Then {
            statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            body("status", Is("FEILET"))
            body("melding", Is("Feilet"))
        }
    }
}
