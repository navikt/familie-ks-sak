package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.ks.sak.api.dto.JournalpostBrukerDto
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI

internal class IntegrasjonClientTest {
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var integrasjonClient: IntegrasjonClient
    private lateinit var wiremockServerItem: WireMockServer

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        integrasjonClient = IntegrasjonClient(URI.create(wiremockServerItem.baseUrl()), restOperations)
    }

    @Test
    fun `hentOppgaver skal returnere en liste av oppgaver basert på request og tema`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/oppgave/v4"))
                .willReturn(WireMock.okJson(readFile("hentOppgaverEnkelKONResponse.json"))),
        )

        // Act
        val oppgaver = integrasjonClient.hentOppgaver(FinnOppgaveRequest(Tema.KON))

        // Assert
        assertThat(oppgaver.antallTreffTotalt).isEqualTo(1)
        assertThat(oppgaver.oppgaver).hasSize(1)
        assertThat(oppgaver.oppgaver.single().tema).isEqualTo(Tema.KON)
    }

    @Test
    fun `hentBehandlendeEnhet skal hente enhet fra familie-integrasjon`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/arbeidsfordeling/enhet/KON"))
                .willReturn(WireMock.okJson(readFile("hentBehandlendeEnhetEnkelResponse.json"))),
        )

        // Act
        val behandlendeEnheter = integrasjonClient.hentBehandlendeEnheter("testident")

        // Assert
        assertThat(behandlendeEnheter).hasSize(2)
        assertThat(behandlendeEnheter.map { it.enhetId }).containsExactlyInAnyOrder("enhetId1", "enhetId2")
        assertThat(behandlendeEnheter.map { it.enhetNavn }).containsExactlyInAnyOrder("enhetNavn1", "enhetNavn2")
    }

    @Test
    fun `hentNavKontorEnhet skal hente enhet fra familie-integrasjon`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/arbeidsfordeling/nav-kontor/200"))
                .willReturn(WireMock.okJson(readFile("hentEnhetEnkelResponse.json"))),
        )

        // Act
        val navKontorEnhet = integrasjonClient.hentNavKontorEnhet("200")

        // Assert
        assertThat(navKontorEnhet.enhetId).isEqualTo(200)
        assertThat(navKontorEnhet.enhetNr).isEqualTo("200")
        assertThat(navKontorEnhet.navn).isEqualTo("Riktig navn")
        assertThat(navKontorEnhet.status).isEqualTo("Riktig status")
    }

    @Test
    fun `finnOppgaveMedId skal hente oppgave med spesifikt id fra familie-integrasjon`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/oppgave/200"))
                .willReturn(WireMock.okJson(readFile("finnOppgaveMedIdEnkelResponse.json"))),
        )

        // Act
        val oppgave = integrasjonClient.finnOppgaveMedId(200)

        // Assert
        assertThat(oppgave.id).isEqualTo(200)
        assertThat(oppgave.tildeltEnhetsnr).isEqualTo("4812")
        assertThat(oppgave.endretAvEnhetsnr).isEqualTo("4812")
        assertThat(oppgave.journalpostId).isEqualTo("123456789")
        assertThat(oppgave.tema).isEqualTo(Tema.KON)
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med true hvis SB har tilgang til alle personidenter`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/tilgang/v2/personer"))
                .willReturn(WireMock.okJson(readFile("sjekkTilgangTilPersonerResponseMedTilgangTilAlle.json"))),
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        // Act
        val tilgangTilPersonIdent = integrasjonClient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        // Assert
        assertThat(tilgangTilPersonIdent.all { it.harTilgang }).isTrue()
        assertThat(tilgangTilPersonIdent.all { it.begrunnelse == "Har tilgang" }).isTrue()
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med false hvis SB ikke har tilgang til alle personidenter`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/tilgang/v2/personer"))
                .willReturn(WireMock.okJson(readFile("sjekkTilgangTilPersonerResponseMedIkkeTilgangTilAlle.json"))),
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        // Act
        val tilgangTilPersonIdent = integrasjonClient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        assertThat(tilgangTilPersonIdent.all { it.harTilgang }).isFalse()
        assertThat(tilgangTilPersonIdent.any { it.begrunnelse == "Har ikke tilgang" }).isTrue()
        assertThat(tilgangTilPersonIdent.any { it.begrunnelse == "Har tilgang" }).isTrue()
    }

    @Test
    fun `fordelOppgave skal returnere fordelt oppgave ved OK fordelelse av oppgave`() {
        // Arrange
        val saksbehandler = "testSB"

        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/oppgave/200/fordel?saksbehandler=$saksbehandler"))
                .willReturn(WireMock.okJson(readFile("fordelOppgaveEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonClient.fordelOppgave(200, saksbehandler)

        // Assert
        assertThat(fordeltOppgave.oppgaveId).isEqualTo(200)
    }

    @Test
    fun `tilordneEnhetForOppgave skal returnere tildelt oppgave ved OK fordelelse av enhet`() {
        // Arrange
        val nyEnhet = "testenhet"

        wiremockServerItem.stubFor(
            WireMock
                .patch(WireMock.urlEqualTo("/oppgave/200/enhet/testenhet?fjernMappeFraOppgave=true"))
                .willReturn(WireMock.okJson(readFile("fordelOppgaveEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonClient.tilordneEnhetForOppgave(200, nyEnhet)

        // Assert
        assertThat(fordeltOppgave.oppgaveId).isEqualTo(200)
    }

    @Test
    fun `leggTilLogiskVedlegg skal returnere id på vedlegg som ble lagt til`() {
        // Arrange
        val request = LogiskVedleggRequest("testtittel")

        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/arkiv/dokument/testid/logiskVedlegg"))
                .willReturn(WireMock.okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonClient.leggTilLogiskVedlegg(request, "testid")

        // Assert
        assertThat(fordeltOppgave.logiskVedleggId).isEqualTo(200)
    }

    @Test
    fun `slettLogiskVedlegg skal returnere id på vedlegg som ble slettet til`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .delete(WireMock.urlEqualTo("/arkiv/dokument/testDokumentId/logiskVedlegg/testId"))
                .willReturn(WireMock.okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonClient.slettLogiskVedlegg("testId", "testDokumentId")

        // Assert
        assertThat(fordeltOppgave.logiskVedleggId).isEqualTo(200)
    }

    @Test
    fun `oppdaterJournalpost skal returnere id på journalpost som ble oppdatert`() {
        // Arrange
        val request = OppdaterJournalpostRequestDto(bruker = JournalpostBrukerDto(id = "testId", navn = "testNavn"))

        wiremockServerItem.stubFor(
            WireMock
                .put(WireMock.urlEqualTo("/arkiv/v2/testJournalpostId"))
                .willReturn(WireMock.okJson(readFile("oppdaterJournalpostEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonClient.oppdaterJournalpost(request, "testJournalpostId")

        // Assert
        assertThat(fordeltOppgave.journalpostId).isEqualTo("oppdatertJournalpostId")
    }

    @Test
    fun `ferdigstillJournalpost skal sette journalpost til ferdigstilt`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .put(WireMock.urlEqualTo("/arkiv/v2/testJournalPost/ferdigstill?journalfoerendeEnhet=testEnhet"))
                .willReturn(WireMock.okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        // Act
        integrasjonClient.ferdigstillJournalpost("testJournalPost", "testEnhet")
    }

    @Test
    fun `journalførDokument skal returnere detaljer om dokument som ble journalført`() {
        // Arrange
        val request = ArkiverDokumentRequest(randomFnr(), true, emptyList(), emptyList())

        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/arkiv/v4"))
                .willReturn(WireMock.okJson(readFile("journalførDokumentEnkelResponse.json"))),
        )

        // Act
        val arkiverDokumentResponse = integrasjonClient.journalførDokument(request)

        assertThat(arkiverDokumentResponse.ferdigstilt).isTrue()
        assertThat(arkiverDokumentResponse.journalpostId).isEqualTo("12345678")
    }

    @Test
    fun `hentLand skal returnere landNavn gitt landKode`() {
        // Arrange
        val landKode = "NOR"

        wiremockServerItem.stubFor(
            WireMock
                .get(WireMock.urlEqualTo("/kodeverk/landkoder/$landKode"))
                .willReturn(WireMock.okJson(readFile("hentLandEnkelResponse.json"))),
        )

        // Act
        val hentLandKodeRespons = integrasjonClient.hentLand(landKode)

        // Assert
        assertThat(hentLandKodeRespons).isEqualTo("Norge")
    }

    @Test
    fun `distribuerBrev skal en bestillingsid på at brevet at distribuert`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/dist/v1"))
                .willReturn(WireMock.okJson(readFile("distribuerBrevEnkelResponse.json"))),
        )

        // Act
        val bestillingId = integrasjonClient.distribuerBrev("testId", Distribusjonstype.VEDTAK)

        // Assert
        assertThat(bestillingId).isEqualTo("testBestillingId")
    }

    @Test
    fun `hentEnheterSomNavIdentHarTilgangTil - skal hente enheter som NAV-ident har tilgang til`() {
        // Arrange
        val navIdent = NavIdent("1")

        wiremockServerItem.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/enhetstilganger"))
                .willReturn(WireMock.okJson(readFile("enheterNavIdentHarTilgangTilResponse.json"))),
        )

        // Act
        val enheter = integrasjonClient.hentEnheterSomNavIdentHarTilgangTil(navIdent)

        // Assert
        assertThat(enheter).hasSize(2)
        assertThat(enheter).anySatisfy {
            assertThat(it.enhetsnummer).isEqualTo("1234")
            assertThat(it.enhetsnavn).isEqualTo("Enhetsnavn1")
        }
        assertThat(enheter).anySatisfy {
            assertThat(it.enhetsnummer).isEqualTo("4321")
            assertThat(it.enhetsnavn).isEqualTo("Enhetsnavn2")
        }
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/familieintegrasjon/json/$filnavn").readText()
}
