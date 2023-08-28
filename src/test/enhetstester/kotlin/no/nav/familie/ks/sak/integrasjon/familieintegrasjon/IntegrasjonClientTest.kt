package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.ks.sak.api.dto.JournalpostBrukerDto
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import org.hamcrest.CoreMatchers.`is` as Is

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
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/oppgave/v4"))
                .willReturn(WireMock.okJson(readFile("hentOppgaverEnkelKONResponse.json"))),
        )

        val oppgaver = integrasjonClient.hentOppgaver(FinnOppgaveRequest(Tema.KON))

        assertThat(oppgaver.antallTreffTotalt, Is(1))
        assertThat(oppgaver.oppgaver.size, Is(1))

        assertThat(oppgaver.oppgaver.single().tema, Is(Tema.KON))
    }

    @Test
    fun `hentBehandlendeEnhet skal hente enhet fra familie-integrasjon`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/arbeidsfordeling/enhet/KON"))
                .willReturn(WireMock.okJson(readFile("hentBehandlendeEnhetEnkelResponse.json"))),
        )

        val behandlendeEnheter = integrasjonClient.hentBehandlendeEnheter("testident")

        assertThat(behandlendeEnheter.size, Is(2))
        assertThat(behandlendeEnheter.map { it.enhetId }, containsInAnyOrder("enhetId1", "enhetId2"))
        assertThat(behandlendeEnheter.map { it.enhetNavn }, containsInAnyOrder("enhetNavn1", "enhetNavn2"))
    }

    @Test
    fun `hentNavKontorEnhet skal hente enhet fra familie-integrasjon`() {
        wiremockServerItem.stubFor(
            WireMock.get(WireMock.urlEqualTo("/arbeidsfordeling/nav-kontor/200"))
                .willReturn(WireMock.okJson(readFile("hentEnhetEnkelResponse.json"))),
        )

        val navKontorEnhet = integrasjonClient.hentNavKontorEnhet("200")

        assertThat(navKontorEnhet.enhetId, Is(200))
        assertThat(navKontorEnhet.enhetNr, Is("200"))
        assertThat(navKontorEnhet.navn, Is("Riktig navn"))
        assertThat(navKontorEnhet.status, Is("Riktig status"))
    }

    @Test
    fun `finnOppgaveMedId skal hente oppgave med spesifikt id fra familie-integrasjon`() {
        wiremockServerItem.stubFor(
            WireMock.get(WireMock.urlEqualTo("/oppgave/200"))
                .willReturn(WireMock.okJson(readFile("finnOppgaveMedIdEnkelResponse.json"))),
        )

        val oppgave = integrasjonClient.finnOppgaveMedId(200)

        assertThat(oppgave.id, Is(200))
        assertThat(oppgave.tildeltEnhetsnr, Is("4812"))
        assertThat(oppgave.endretAvEnhetsnr, Is("4812"))
        assertThat(oppgave.journalpostId, Is("123456789"))
        assertThat(oppgave.tema, Is(Tema.KON))
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med true hvis SB har tilgang til alle personidenter`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/tilgang/v2/personer"))
                .willReturn(WireMock.okJson(readFile("sjekkTilgangTilPersonerResponseMedTilgangTilAlle.json"))),
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        val tilgangTilPersonIdent = integrasjonClient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        assertThat(tilgangTilPersonIdent.harTilgang, Is(true))
        assertThat(tilgangTilPersonIdent.begrunnelse, Is("Har tilgang"))
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med false hvis SB ikke har tilgang til alle personidenter`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/tilgang/v2/personer"))
                .willReturn(WireMock.okJson(readFile("sjekkTilgangTilPersonerResponseMedIkkeTilgangTilAlle.json"))),
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        val tilgangTilPersonIdent = integrasjonClient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        assertThat(tilgangTilPersonIdent.harTilgang, Is(false))
        assertThat(tilgangTilPersonIdent.begrunnelse, Is("Har ikke tilgang"))
    }

    @Test
    fun `fordelOppgave skal returnere fordelt oppgave ved OK fordelelse av oppgave`() {
        val saksbehandler = "testSB"

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/oppgave/200/fordel?saksbehandler=$saksbehandler"))
                .willReturn(WireMock.okJson(readFile("fordelOppgaveEnkelResponse.json"))),
        )

        val fordeltOppgave = integrasjonClient.fordelOppgave(200, saksbehandler)

        assertThat(fordeltOppgave.oppgaveId, Is(200))
    }

    @Test
    fun `tilordneEnhetForOppgave skal returnere tildelt oppgave ved OK fordelelse av enhet`() {
        val nyEnhet = "testenhet"

        wiremockServerItem.stubFor(
            WireMock.patch(WireMock.urlEqualTo("/oppgave/200/enhet/testenhet?fjernMappeFraOppgave=true"))
                .willReturn(WireMock.okJson(readFile("fordelOppgaveEnkelResponse.json"))),
        )

        val fordeltOppgave = integrasjonClient.tilordneEnhetForOppgave(200, nyEnhet)

        assertThat(fordeltOppgave.oppgaveId, Is(200))
    }

    @Test
    fun `leggTilLogiskVedlegg skal returnere id på vedlegg som ble lagt til`() {
        val request = LogiskVedleggRequest("testtittel")

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/arkiv/dokument/testid/logiskVedlegg"))
                .willReturn(WireMock.okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        val fordeltOppgave = integrasjonClient.leggTilLogiskVedlegg(request, "testid")

        assertThat(fordeltOppgave.logiskVedleggId, Is(200))
    }

    @Test
    fun `slettLogiskVedlegg skal returnere id på vedlegg som ble slettet til`() {
        wiremockServerItem.stubFor(
            WireMock.delete(WireMock.urlEqualTo("/arkiv/dokument/testDokumentId/logiskVedlegg/testId"))
                .willReturn(WireMock.okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        val fordeltOppgave = integrasjonClient.slettLogiskVedlegg("testId", "testDokumentId")

        assertThat(fordeltOppgave.logiskVedleggId, Is(200))
    }

    @Test
    fun `oppdaterJournalpost skal returnere id på journalpost som ble oppdatert`() {
        val request = OppdaterJournalpostRequestDto(bruker = JournalpostBrukerDto(id = "testId", navn = "testNavn"))

        wiremockServerItem.stubFor(
            WireMock.put(WireMock.urlEqualTo("/arkiv/v2/testJournalpostId"))
                .willReturn(WireMock.okJson(readFile("oppdaterJournalpostEnkelResponse.json"))),
        )

        val fordeltOppgave = integrasjonClient.oppdaterJournalpost(request, "testJournalpostId")

        assertThat(fordeltOppgave.journalpostId, Is("oppdatertJournalpostId"))
    }

    @Test
    fun `ferdigstillJournalpost skal sette journalpost til ferdigstilt`() {
        wiremockServerItem.stubFor(
            WireMock.put(WireMock.urlEqualTo("/arkiv/v2/testJournalPost/ferdigstill?journalfoerendeEnhet=testEnhet"))
                .willReturn(WireMock.okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        integrasjonClient.ferdigstillJournalpost("testJournalPost", "testEnhet")
    }

    @Test
    fun `journalførDokument skal returnere detaljer om dokument som ble journalført`() {
        val request = ArkiverDokumentRequest(randomFnr(), true, emptyList(), emptyList())

        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/arkiv/v4"))
                .willReturn(WireMock.okJson(readFile("journalførDokumentEnkelResponse.json"))),
        )

        val arkiverDokumentResponse = integrasjonClient.journalførDokument(request)

        assertThat(arkiverDokumentResponse.ferdigstilt, Is(true))
        assertThat(arkiverDokumentResponse.journalpostId, Is("12345678"))
    }

    @Test
    fun `hentLand skal returnere landNavn gitt landKode`() {
        val landKode = "NOR"

        wiremockServerItem.stubFor(
            WireMock.get(WireMock.urlEqualTo("/kodeverk/landkoder/$landKode"))
                .willReturn(WireMock.okJson(readFile("hentLandEnkelResponse.json"))),
        )

        val hentLandKodeRespons = integrasjonClient.hentLand(landKode)

        assertThat(hentLandKodeRespons, Is("Norge"))
    }

    @Test
    fun `distribuerBrev skal en bestillingsid på at brevet at distribuert`() {
        wiremockServerItem.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dist/v1"))
                .willReturn(WireMock.okJson(readFile("distribuerBrevEnkelResponse.json"))),
        )

        val bestillingId = integrasjonClient.distribuerBrev("testId", Distribusjonstype.VEDTAK)

        assertThat(bestillingId, Is("testBestillingId"))
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/familieintegrasjon/json/$filnavn").readText()
    }
}
