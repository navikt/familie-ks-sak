package no.nav.familie.ks.sak.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import org.hamcrest.CoreMatchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    val behandlingControllerUrl = "/api/behandlinger"

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "test",
                behandlendeEnhetNavn = "test",
            ),
        )

        every { integrasjonClient.hentLand(any()) } returns "Norge"
    }

    @Test
    fun `hentBehandling - skal returnere 401 dersom brukeren ikke har token for å hente behandling`() {
        When {
            get("$behandlingControllerUrl/401")
        } Then {
            statusCode(HttpStatus.UNAUTHORIZED.value())
        }
    }

    @Test
    fun `hentBehandling - skal returnere 403 dersom brukeren ikke har rettigheter til å hente en behandling`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.UKJENT)

        Given {
            header("Authorization", "Bearer $token")
        } When {
            get("$behandlingControllerUrl/403")
        } Then {
            statusCode(HttpStatus.FORBIDDEN.value())
        }
    }

    @Test
    fun `hentBehandling - skal returnere BehandlingResponsDto og 200 OK dersom behandling finnes`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))

        Given {
            header("Authorization", "Bearer $token")
        } When {
            get("$behandlingControllerUrl/${behandling.id}")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("data.behandlingId", Is(behandling.id.toInt()))
            body("data.årsak", Is("SØKNAD"))
        }
    }

    @Test
    fun `endreBehandlendeEnhet - skal kaste FunksjonellFeil hvis begrunnelse er tom`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(objectMapper.writeValueAsString(EndreBehandlendeEnhetDto("enhet", "")))
        } When {
            put("$behandlingControllerUrl/${behandling.id}/enhet")
        } Then {
            body("status", Is("FUNKSJONELL_FEIL"))
            body("melding", Is("Begrunnelse kan ikke være tom"))
            body("frontendFeilmelding", Is("Du må skrive en begrunnelse for endring av enhet"))
        }
    }

    @Test
    fun `endreBehandlendeEnhet - skal returnere behandling med endret enhet`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))
        every { integrasjonClient.hentNavKontorEnhet("50") } returns
            NavKontorEnhet(
                50,
                "nyNavn",
                "nyEnhetNr",
                "nyStatus",
            )

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(objectMapper.writeValueAsString(EndreBehandlendeEnhetDto("50", "nybegrunnelse")))
        } When {
            put("$behandlingControllerUrl/${behandling.id}/enhet")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("data.arbeidsfordelingPåBehandling.behandlendeEnhetId", Is("50"))
            body("data.arbeidsfordelingPåBehandling.behandlendeEnhetNavn", Is("nyNavn"))
            body("data.arbeidsfordelingPåBehandling.manueltOverstyrt", Is(true))
        }
    }

    @Test
    fun `opprettBehandling - skal kaste funksjonell feil hvis fagsak allerede har en aktiv behandling som ikke er ferdigstilt`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)

        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))

        Given {
            header("Authorization", "Bearer $token")
            contentType(ContentType.JSON)
            body(
                objectMapper.writeValueAsString(
                    OpprettBehandlingDto(
                        søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                        kategori = BehandlingKategori.NASJONAL,
                    ),
                ),
            )
        } When {
            post(behandlingControllerUrl)
        } Then {
            body("status", Is("FUNKSJONELL_FEIL"))
            body(
                "melding",
                Is("Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt."),
            )
            body(
                "frontendFeilmelding",
                Is("Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt."),
            )
        }
    }

    @Test
    fun `hentBehandlinger - skal returnere MinimalBehandlingResponsDto og 200 OK dersom behandlinger finnes`() {
        val token = lokalTestToken(behandlerRolle = BehandlerRolle.BESLUTTER)
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang("test", true))

        Given {
            header("Authorization", "Bearer $token")
        } When {
            get("$behandlingControllerUrl/fagsak/${fagsak.id}")
        } Then {
            statusCode(HttpStatus.OK.value())
            body("data.behandlingId", hasItem(behandling.id.toInt()))
            body("data.årsak", hasItem("SØKNAD"))
        }
    }
}
