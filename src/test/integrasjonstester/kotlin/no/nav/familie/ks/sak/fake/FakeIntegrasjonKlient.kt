package no.nav.familie.ks.sak.fake

import io.mockk.mockk
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.kodeverk.BeskrivelseDto
import no.nav.familie.kontrakter.felles.kodeverk.BetydningDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkSpråk
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.datagenerator.lagKodeverkLand
import no.nav.familie.ks.sak.datagenerator.lagTestJournalpost
import no.nav.familie.ks.sak.datagenerator.lagTestOppgaveDTO
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import org.springframework.core.io.ClassPathResource
import org.springframework.web.client.RestOperations
import tools.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class FakeIntegrasjonKlient(
    restOperations: RestOperations,
) : IntegrasjonKlient(URI("integrasjoner-url"), restOperations, mockk(relaxed = true), 1) {
    private val egenansatt = mutableSetOf<String>()
    private val behandlendeEnhetForIdent = mutableMapOf<String, List<Arbeidsfordelingsenhet>>()
    private val journalførteDokumenter = mutableListOf<ArkiverDokumentRequest>()
    private val personIdentTilTilgang = mutableMapOf<String, Tilgang>()
    private val kallMotSjekkTilgangTilPersoner: MutableList<List<String>> = mutableListOf()
    private var godkjennByDefault: Boolean = true

    override fun hentAlleEØSLand(): KodeverkDto = lagKodeverkLand()

    override fun hentLand(landkode: String): String = "Testland"

    override fun sjekkTilgangTilPersoner(personIdenter: List<String>): List<Tilgang> {
        kallMotSjekkTilgangTilPersoner.add(personIdenter)
        return personIdenter.map { personIdent ->
            personIdentTilTilgang[personIdent] ?: Tilgang(personIdent, godkjennByDefault)
        }
    }

    override fun hentPoststeder(): KodeverkDto =
        KodeverkDto(
            betydninger =
                (0..9999).associate {
                    it.toString().padStart(4, '0') to
                        listOf(
                            BetydningDto(
                                gyldigFra = LocalDate.now().minusYears(1),
                                gyldigTil = LocalDate.now().plusYears(1),
                                beskrivelser = mapOf(KodeverkSpråk.BOKMÅL.kode to BeskrivelseDto("Oslo", "Oslo")),
                            ),
                        )
                },
        )

    override fun hentBehandlendeEnheterSomNavIdentHarTilgangTil(navIdent: NavIdent): List<KontantstøtteEnhet> = KontantstøtteEnhet.entries

    override fun ferdigstillOppgave(oppgaveId: Long) {
        return
    }

    override fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse = OppgaveResponse(12345678L)

    override fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String?,
    ): OppgaveResponse = OppgaveResponse(12345678L)

    override fun tilordneEnhetOgRessursForOppgave(
        oppgaveId: Long,
        nyEnhet: String,
    ): OppgaveResponse = OppgaveResponse(12345678L)

    override fun finnOppgaveMedId(oppgaveId: Long): Oppgave = lagTestOppgaveDTO(oppgaveId)

    override fun hentJournalpost(journalpostId: String): Journalpost =
        lagTestJournalpost(
            journalpostId = journalpostId,
            personIdent = randomFnr(),
            avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
            kanal = "NAV_NO",
        )

    override fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        val søkerFnr = randomFnr()
        return listOf(
            lagTestJournalpost(
                personIdent = søkerFnr,
                journalpostId = UUID.randomUUID().toString(),
                avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                kanal = "NAV_NO",
            ),
            lagTestJournalpost(
                personIdent = søkerFnr,
                journalpostId = UUID.randomUUID().toString(),
                avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                kanal = "NAV_NO",
            ),
        )
    }

    override fun hentBehandlendeEnheter(
        ident: String,
        behandlingstype: Behandlingstype?,
    ): List<Arbeidsfordelingsenhet> =
        behandlendeEnhetForIdent[ident] ?: listOf(
            Arbeidsfordelingsenhet.opprettFra(KontantstøtteEnhet.OSLO),
        )

    override fun hentTilgangsstyrteJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<TilgangsstyrtJournalpost> = emptyList()

    override fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto =
        FinnOppgaveResponseDto(
            2,
            listOf(lagTestOppgaveDTO(1L), lagTestOppgaveDTO(2L, Oppgavetype.BehandleSak, "Z999999")),
        )

    override fun ferdigstillJournalpost(
        journalpostId: String,
        journalførendeEnhet: String,
    ) {
        return
    }

    override fun leggTilLogiskVedlegg(
        request: LogiskVedleggRequest,
        dokumentinfoId: String,
    ): LogiskVedleggResponse = LogiskVedleggResponse(12345678L)

    override fun slettLogiskVedlegg(
        logiskVedleggId: String,
        dokumentinfoId: String,
    ): LogiskVedleggResponse = LogiskVedleggResponse(12345678L)

    override fun journalførDokument(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        journalførteDokumenter.add(arkiverDokumentRequest)
        return ArkiverDokumentResponse(ferdigstilt = true, journalpostId = "journalpostId")
    }

    override fun hentLandkoderISO2(): Map<String, String> = hentLandkoder()

    override fun hentAInntektUrl(personIdent: PersonIdent): String = "/test/1234"

    override fun hentSaksbehandler(id: String): Saksbehandler =
        Saksbehandler(
            azureId = UUID.randomUUID(),
            navIdent = id,
            fornavn = "System",
            etternavn = "",
            enhet = KontantstøtteEnhet.OSLO.enhetsnummer,
            enhetsnavn = KontantstøtteEnhet.OSLO.enhetsnavn,
        )

    override fun hentNavKontorEnhet(enhetId: String?): NavKontorEnhet =
        NavKontorEnhet(
            1,
            "navKontor",
            "navEnhet",
            "navStatus",
        )

    private fun hentLandkoder(): Map<String, String> {
        val landkoder =
            ClassPathResource("landkoder/landkoder.json").inputStream.bufferedReader().use(BufferedReader::readText)

        return jsonMapper.readValue<List<LandkodeISO2>>(landkoder).associate { it.code to it.name }
    }

    /**
     * Legger til tilgang for testIdenter og setter defaulten for godkjenning til false
     *
     * VIKTIG at man resetter godkjennDefault tilbake til true i etterkant, hvis ikke feiler påfølgende tester som trenger at den er satt til true
     */
    fun leggTilTilganger(
        personIdentTilHarTilgang: List<Tilgang>,
        godkjennDefault: Boolean = false,
    ) {
        personIdentTilTilgang.putAll(personIdentTilHarTilgang.associate { tilgang -> tilgang.personIdent to tilgang })
        godkjennByDefault = godkjennDefault
    }

    fun reset() {
        personIdentTilTilgang.clear()
        kallMotSjekkTilgangTilPersoner.clear()
        godkjennByDefault = true
    }

    data class LandkodeISO2(
        val code: String,
        val name: String,
    )
}
