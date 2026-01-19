package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.ks.sak.api.dto.JournalpostBrukerDto
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle.HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.lagAvsenderMottaker
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI

internal class IntegrasjonKlientTest {
    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private lateinit var integrasjonKlient: IntegrasjonKlient
    private lateinit var wiremockServerItem: WireMockServer
    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun initClass() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        integrasjonKlient = IntegrasjonKlient(URI.create(wiremockServerItem.baseUrl()), restOperations, featureToggleService)
    }

    @AfterEach
    fun tearDown() {
        wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wiremockServerItem.start()
        integrasjonKlient = IntegrasjonKlient(URI.create(wiremockServerItem.baseUrl()), restOperations, featureToggleService)
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `hentOppgaver skal returnere en liste av oppgaver basert på request og tema`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/oppgave/v4"))
                .willReturn(okJson(readFile("hentOppgaverEnkelKONResponse.json"))),
        )

        // Act
        val oppgaver = integrasjonKlient.hentOppgaver(FinnOppgaveRequest(Tema.KON))

        // Assert
        assertThat(oppgaver.antallTreffTotalt).isEqualTo(1)
        assertThat(oppgaver.oppgaver).hasSize(1)
        assertThat(oppgaver.oppgaver.single().tema).isEqualTo(Tema.KON)
    }

    @Test
    fun `hentBehandlendeEnhet skal hente enhet fra familie-integrasjon uten behandlingstype`() {
        // Arrange
        every { featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE) } returns false
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/arbeidsfordeling/enhet/KON"))
                .willReturn(okJson(readFile("hentBehandlendeEnhetEnkelResponse.json"))),
        )

        // Act
        val behandlendeEnheter = integrasjonKlient.hentBehandlendeEnheter("testident")

        // Assert
        assertThat(behandlendeEnheter).hasSize(2)
        assertThat(behandlendeEnheter.map { it.enhetId }).containsExactlyInAnyOrder("enhetId1", "enhetId2")
        assertThat(behandlendeEnheter.map { it.enhetNavn }).containsExactlyInAnyOrder("enhetNavn1", "enhetNavn2")
    }

    @Test
    fun `hentBehandlendeEnhet skal hente enhet fra familie-integrasjon med behandlingstype`() {
        // Arrange
        every { featureToggleService.isEnabled(HENT_ARBEIDSFORDELING_MED_BEHANDLINGSTYPE) } returns true
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/arbeidsfordeling/enhet/KON?behandlingstype=NASJONAL"))
                .willReturn(okJson(readFile("hentBehandlendeEnhetEnkelResponse.json"))),
        )

        // Act
        val behandlendeEnheter = integrasjonKlient.hentBehandlendeEnheter("testident", Behandlingstype.NASJONAL)

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
                .get(urlEqualTo("/arbeidsfordeling/nav-kontor/200"))
                .willReturn(okJson(readFile("hentEnhetEnkelResponse.json"))),
        )

        // Act
        val navKontorEnhet = integrasjonKlient.hentNavKontorEnhet("200")

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
                .get(urlEqualTo("/oppgave/200"))
                .willReturn(okJson(readFile("finnOppgaveMedIdEnkelResponse.json"))),
        )

        // Act
        val oppgave = integrasjonKlient.finnOppgaveMedId(200)

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
                .post(urlEqualTo("/tilgang/v2/personer"))
                .willReturn(okJson(readFile("sjekkTilgangTilPersonerResponseMedTilgangTilAlle.json"))),
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        // Act
        val tilgangTilPersonIdent = integrasjonKlient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

        // Assert
        assertThat(tilgangTilPersonIdent.all { it.harTilgang }).isTrue()
        assertThat(tilgangTilPersonIdent.all { it.begrunnelse == "Har tilgang" }).isTrue()
    }

    @Test
    fun `sjekkTilgangTilPersoner skal returnere Tilgang med false hvis SB ikke har tilgang til alle personidenter`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/tilgang/v2/personer"))
                .willReturn(okJson(readFile("sjekkTilgangTilPersonerResponseMedIkkeTilgangTilAlle.json"))),
        )

        mockkObject(SikkerhetContext)
        every { SikkerhetContext.erSystemKontekst() } returns false

        // Act
        val tilgangTilPersonIdent = integrasjonKlient.sjekkTilgangTilPersoner(listOf("ident1", "ident2", "ident3"))

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
                .post(urlEqualTo("/oppgave/200/fordel?saksbehandler=$saksbehandler"))
                .willReturn(okJson(readFile("fordelOppgaveEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonKlient.fordelOppgave(200, saksbehandler)

        // Assert
        assertThat(fordeltOppgave.oppgaveId).isEqualTo(200)
    }

    @Test
    fun `tilordneEnhetOgRessursForOppgave skal returnere tildelt oppgave ved OK fordelelse av enhet`() {
        // Arrange
        val nyEnhet = "testenhet"

        wiremockServerItem.stubFor(
            WireMock
                .patch(urlEqualTo("/oppgave/200/enhet/testenhet?fjernMappeFraOppgave=true&nullstillTilordnetRessurs=true"))
                .willReturn(okJson(readFile("fordelOppgaveEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonKlient.tilordneEnhetOgRessursForOppgave(200, nyEnhet)

        // Assert
        assertThat(fordeltOppgave.oppgaveId).isEqualTo(200)
    }

    @Test
    fun `leggTilLogiskVedlegg skal returnere id på vedlegg som ble lagt til`() {
        // Arrange
        val request = LogiskVedleggRequest("testtittel")

        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/arkiv/dokument/testid/logiskVedlegg"))
                .willReturn(okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonKlient.leggTilLogiskVedlegg(request, "testid")

        // Assert
        assertThat(fordeltOppgave.logiskVedleggId).isEqualTo(200)
    }

    @Test
    fun `slettLogiskVedlegg skal returnere id på vedlegg som ble slettet til`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .delete(urlEqualTo("/arkiv/dokument/testDokumentId/logiskVedlegg/testId"))
                .willReturn(okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonKlient.slettLogiskVedlegg("testId", "testDokumentId")

        // Assert
        assertThat(fordeltOppgave.logiskVedleggId).isEqualTo(200)
    }

    @Test
    fun `oppdaterJournalpost skal returnere id på journalpost som ble oppdatert`() {
        // Arrange
        val request = OppdaterJournalpostRequestDto(bruker = JournalpostBrukerDto(id = "testId", navn = "testNavn"))

        wiremockServerItem.stubFor(
            WireMock
                .put(urlEqualTo("/arkiv/v2/testJournalpostId"))
                .willReturn(okJson(readFile("oppdaterJournalpostEnkelResponse.json"))),
        )

        // Act
        val fordeltOppgave = integrasjonKlient.oppdaterJournalpost(request, "testJournalpostId")

        // Assert
        assertThat(fordeltOppgave.journalpostId).isEqualTo("oppdatertJournalpostId")
    }

    @Test
    fun `ferdigstillJournalpost skal sette journalpost til ferdigstilt`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .put(urlEqualTo("/arkiv/v2/testJournalPost/ferdigstill?journalfoerendeEnhet=testEnhet"))
                .willReturn(okJson(readFile("logiskVedleggEnkelResponse.json"))),
        )

        // Act
        integrasjonKlient.ferdigstillJournalpost("testJournalPost", "testEnhet")
    }

    @Test
    fun `journalførDokument skal returnere detaljer om dokument som ble journalført`() {
        // Arrange
        val request = ArkiverDokumentRequest(randomFnr(), true, emptyList(), emptyList())

        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/arkiv/v4"))
                .willReturn(okJson(readFile("journalførDokumentEnkelResponse.json"))),
        )

        // Act
        val arkiverDokumentResponse = integrasjonKlient.journalførDokument(request)

        assertThat(arkiverDokumentResponse.ferdigstilt).isTrue()
        assertThat(arkiverDokumentResponse.journalpostId).isEqualTo("12345678")
    }

    @Test
    fun `hentLand skal returnere landNavn gitt landKode`() {
        // Arrange
        val landKode = "NOR"

        wiremockServerItem.stubFor(
            WireMock
                .get(urlEqualTo("/kodeverk/landkoder/$landKode"))
                .willReturn(okJson(readFile("hentLandEnkelResponse.json"))),
        )

        // Act
        val hentLandKodeRespons = integrasjonKlient.hentLand(landKode)

        // Assert
        assertThat(hentLandKodeRespons).isEqualTo("Norge")
    }

    @Test
    fun `distribuerBrev skal en bestillingsid på at brevet at distribuert`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/dist/v1"))
                .willReturn(okJson(readFile("distribuerBrevEnkelResponse.json"))),
        )

        // Act
        val bestillingId = integrasjonKlient.distribuerBrev("testId", Distribusjonstype.VEDTAK)

        // Assert
        assertThat(bestillingId).isEqualTo("testBestillingId")
    }

    @Test
    fun `hentEnheterSomNavIdentHarTilgangTil - skal hente enheter som NAV-ident har tilgang til`() {
        // Arrange
        val navIdent = NavIdent("1")

        wiremockServerItem.stubFor(
            get(urlEqualTo("/saksbehandler/1/grupper"))
                .willReturn(okJson(readFile("enheterNavIdentHarTilgangTilResponse.json"))),
        )

        // Act
        val enheter = integrasjonKlient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent)

        // Assert
        assertThat(enheter).hasSize(2)
        assertThat(enheter).anySatisfy {
            assertThat(it.enhetsnummer).isEqualTo(KontantstøtteEnhet.VADSØ.enhetsnummer)
            assertThat(it.enhetsnavn).isEqualTo(KontantstøtteEnhet.VADSØ.enhetsnavn)
        }
        assertThat(enheter).anySatisfy {
            assertThat(it.enhetsnummer).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnummer)
            assertThat(it.enhetsnavn).isEqualTo(KontantstøtteEnhet.VIKAFOSSEN.enhetsnavn)
        }
    }

    @Test
    fun `hentTilgangsstyrteJournalposterForBruker - skal hente tilgangsstyrte journalposter for bruker`() {
        // Arrange
        wiremockServerItem.stubFor(
            WireMock
                .post(urlEqualTo("/journalpost/tilgangsstyrt/baks"))
                .willReturn(okJson(readFile("hentTilgangsstyrteJournalposterForBruker.json"))),
        )

        // Act
        val tilgangsstyrteJournalposter = integrasjonKlient.hentTilgangsstyrteJournalposterForBruker(JournalposterForBrukerRequest(brukerId = Bruker(id = "12345678910", type = BrukerIdType.FNR), antall = 100, tema = listOf(Tema.KON)))

        // Assert
        assertThat(tilgangsstyrteJournalposter).hasSize(1)
        val tilgangsstyrtJournalpost = tilgangsstyrteJournalposter.single()
        assertThat(tilgangsstyrtJournalpost.journalpost.journalpostId).isEqualTo("453492634")
        assertThat(tilgangsstyrtJournalpost.journalpost.tema).isEqualTo(Tema.KON.name)
        assertThat(tilgangsstyrtJournalpost.journalpost.kanal).isEqualTo("NAV_NO")
        assertThat(tilgangsstyrtJournalpost.journalpostTilgang.harTilgang).isTrue
    }

    @Test
    fun `hentJournalpost returnerer OK`() {
        val journalpostId = "1234"
        val fnr = randomFnr()
        wiremockServerItem.stubFor(
            get("/journalpost/tilgangsstyrt/baks?journalpostId=$journalpostId").willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        success(
                            lagJournalpost(
                                personIdent = fnr,
                                journalpostId = journalpostId,
                                avsenderMottaker =
                                    lagAvsenderMottaker(
                                        personIdent = fnr,
                                        avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                                        navn = "testNavn",
                                    ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val journalpost = integrasjonKlient.hentJournalpost(journalpostId)
        assertThat(journalpost).isNotNull
        assertThat(journalpost.journalpostId).isEqualTo(journalpostId)
        assertThat(journalpost.bruker?.id).isEqualTo(fnr)

        wiremockServerItem.verify(getRequestedFor(urlEqualTo("/journalpost/tilgangsstyrt/baks?journalpostId=$journalpostId")))
    }

    @Test
    fun `hentDokumentIJournalpost returnerer OK`() {
        val journalpostId = "1234"
        val dokumentId = "5678"
        val fnr = randomFnr()
        wiremockServerItem.stubFor(
            get("/journalpost/hentdokument/tilgangsstyrt/baks/$journalpostId/$dokumentId").willReturn(
                okJson(
                    objectMapper.writeValueAsString(
                        success(
                            "Test".toByteArray(),
                        ),
                    ),
                ),
            ),
        )

        val dokument = integrasjonKlient.hentDokumentIJournalpost(journalpostId = journalpostId, dokumentId = dokumentId)
        assertThat(dokument).isNotNull
        assertThat(dokument.decodeToString()).isEqualTo("Test")

        wiremockServerItem.verify(getRequestedFor(urlEqualTo("/journalpost/hentdokument/tilgangsstyrt/baks/$journalpostId/$dokumentId")))
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/familieintegrasjon/json/$filnavn").readText()
}
