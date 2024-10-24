package no.nav.familie.ks.sak.integrasjon

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.ks.sak.api.dto.Sakstype
import no.nav.familie.ks.sak.integrasjon.journalføring.InnkommendeJournalføringService
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import java.time.LocalDateTime

fun lagTilgangsstyrtJournalpost(
    personIdent: String,
    journalpostId: String,
    kanal: String? = InnkommendeJournalføringService.NAV_NO,
    harTilgang: Boolean = true,
): TilgangsstyrtJournalpost =
    TilgangsstyrtJournalpost(
        journalpost = lagJournalpost(personIdent = personIdent, journalpostId = journalpostId, kanal = kanal),
        harTilgang = harTilgang,
    )

fun lagJournalpost(
    personIdent: String,
    journalpostId: String,
    kanal: String? = InnkommendeJournalføringService.NAV_NO,
): Journalpost =
    Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = Tema.KON.name,
        behandlingstema = "ab00001",
        bruker = Bruker(personIdent, type = BrukerIdType.FNR),
        avsenderMottaker =
            AvsenderMottaker(
                navn = "BLÅØYD HEST",
                erLikBruker = true,
                id = personIdent,
                land = "NO",
                type = AvsenderMottakerIdType.FNR,
            ),
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
