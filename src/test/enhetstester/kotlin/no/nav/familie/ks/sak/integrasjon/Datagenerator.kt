package no.nav.familie.ks.sak.integrasjon

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalpostTilgang
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.ks.sak.api.dto.JournalføringRequestDto
import no.nav.familie.ks.sak.api.dto.JournalpostDokumentDto
import no.nav.familie.ks.sak.api.dto.NavnOgIdentDto
import no.nav.familie.ks.sak.api.dto.Sakstype
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

fun lagTilgangsstyrtJournalpost(
    personIdent: String,
    journalpostId: String,
    kanal: String? = InnkommendeJournalføringService.NAV_NO,
    harTilgang: Boolean = true,
): TilgangsstyrtJournalpost =
    TilgangsstyrtJournalpost(
        journalpost =
            lagJournalpost(
                personIdent = personIdent,
                journalpostId = journalpostId,
                kanal = kanal,
                avsenderMottaker =
                    lagAvsenderMottaker(
                        personIdent = personIdent,
                        avsenderMottakerIdType = AvsenderMottakerIdType.FNR,
                        navn = "BLÅØYD HEST",
                        erLikBruker = true,
                        land = "NO",
                    ),
            ),
        journalpostTilgang = JournalpostTilgang(harTilgang = harTilgang),
    )

fun lagJournalpost(
    personIdent: String,
    journalpostId: String,
    kanal: String? = InnkommendeJournalføringService.NAV_NO,
    avsenderMottaker: AvsenderMottaker?,
): Journalpost =
    Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = Tema.KON.name,
        behandlingstema = "ab00001",
        bruker = Bruker(personIdent, type = BrukerIdType.FNR),
        avsenderMottaker = avsenderMottaker,
        journalforendeEnhet = DEFAULT_JOURNALFØRENDE_ENHET,
        kanal = kanal,
        dokumenter =
            listOf(
                DokumentInfo(
                    tittel = "Søknad om kontantstøtte",
                    brevkode = InnkommendeJournalføringService.SØKNADSKODE_KONTANTSTØTTE,
                    dokumentstatus = null,
                    dokumentvarianter = emptyList(),
                    dokumentInfoId = "1",
                    logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
                ),
                DokumentInfo(
                    tittel = "Ekstra vedlegg",
                    brevkode = null,
                    dokumentstatus = null,
                    dokumentvarianter = emptyList(),
                    dokumentInfoId = "2",
                    logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
                ),
            ),
        sak =
            Sak(
                arkivsaksnummer = "",
                arkivsaksystem = "GSAK",
                sakstype = Sakstype.FAGSAK.name,
                fagsakId = "10695768",
                fagsaksystem = FAGSYSTEM,
            ),
        tittel = "Søknad om ordinær barnetrygd",
        relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
    )

fun lagAvsenderMottaker(
    personIdent: String,
    avsenderMottakerIdType: AvsenderMottakerIdType,
    navn: String,
    erLikBruker: Boolean = false,
    land: String = "NO",
) = AvsenderMottaker(
    navn = navn,
    erLikBruker = erLikBruker,
    id = personIdent,
    land = land,
    type = avsenderMottakerIdType,
)

fun lagJournalføringRequestDto(bruker: NavnOgIdentDto): JournalføringRequestDto =
    JournalføringRequestDto(
        avsender = bruker,
        bruker = bruker,
        datoMottatt = LocalDateTime.now().minusDays(10),
        journalpostTittel = "Søknad om ordinær kontantstøtte",
        kategori = BehandlingKategori.NASJONAL,
        knyttTilFagsak = true,
        opprettOgKnyttTilNyBehandling = true,
        tilknyttedeBehandlingIder = emptyList(),
        dokumenter =
            listOf(
                JournalpostDokumentDto(
                    dokumentTittel = "Søknad om kontantstøtte",
                    brevkode = "mock",
                    dokumentInfoId = "1",
                    logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
                    eksisterendeLogiskeVedlegg = emptyList(),
                ),
                JournalpostDokumentDto(
                    dokumentTittel = "Ekstra vedlegg",
                    brevkode = "mock",
                    dokumentInfoId = "2",
                    logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
                    eksisterendeLogiskeVedlegg = emptyList(),
                ),
            ),
        navIdent = "09123",
        nyBehandlingstype = JournalføringBehandlingstype.FØRSTEGANGSBEHANDLING,
        nyBehandlingsårsak = BehandlingÅrsak.SØKNAD,
        journalførendeEnhet = "4820",
    )
