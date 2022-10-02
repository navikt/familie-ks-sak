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
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.hamcrest.CoreMatchers.`is` as Is

class BehandlingControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun beforeEach() {
        fagsak = lagreFagsak()
        behandling = lagreBehandling(fagsak)
    }

    val behandlingControllerUrl = "/api/behandlinger"

    @BeforeEach
    fun setup() {
        RestAssured.port = port
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
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

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
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")

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
        every { integrasjonClient.sjekkTilgangTilPersoner(any()) } returns Tilgang(true, "test")
        every { integrasjonClient.hentNavKontorEnhet("50") } returns NavKontorEnhet(50, "nyNavn", "nyEnhetNr", "nyStatus")

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

    private fun lagreFagsak(): Fagsak {
        val aktør = aktørRepository.saveAndFlush(randomAktør())

        return fagsakRepository.saveAndFlush(lagFagsak(aktør))
    }

    private fun lagreBehandling(fagsak: Fagsak): Behandling {
        val behandling =
            behandlingRepository.saveAndFlush(lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD))

        arbeidsfordelingPåBehandlingRepository.save(
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "test",
                behandlendeEnhetNavn = "test"
            )
        )

        return behandling
    }
}
