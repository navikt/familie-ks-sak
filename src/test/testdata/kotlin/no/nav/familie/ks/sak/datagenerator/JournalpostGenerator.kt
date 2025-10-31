package no.nav.familie.ks.sak.datagenerator

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
import no.nav.familie.ks.sak.integrasjon.journalføring.UtgåendeJournalføringService.Companion.DEFAULT_JOURNALFØRENDE_ENHET
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

fun lagTestJournalpost(
    personIdent: String,
    journalpostId: String,
    avsenderMottakerIdType: AvsenderMottakerIdType?,
    kanal: String,
    sak: Sak? = lagSak(),
): Journalpost =
    Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = Tema.BAR.name,
        behandlingstema = "ab00001",
        bruker = Bruker(personIdent, type = BrukerIdType.FNR),
        avsenderMottaker =
            AvsenderMottaker(
                navn = "BLÅØYD HEST",
                erLikBruker = true,
                id = personIdent,
                land = "NO",
                type = avsenderMottakerIdType,
            ),
        journalforendeEnhet = DEFAULT_JOURNALFØRENDE_ENHET,
        kanal = kanal,
        dokumenter =
            listOf(
                DokumentInfo(
                    tittel = "Søknad om kontantstøtte",
                    brevkode = "NAV 33-00.07",
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
        sak = sak,
        tittel = "Søknad om ordinær kontantstøtte",
        relevanteDatoer = listOf(RelevantDato(LocalDateTime.now(), "DATO_REGISTRERT")),
    )

private fun lagSak() =
    Sak(
        arkivsaksnummer = "",
        arkivsaksystem = "GSAK",
        sakstype = Sakstype.FAGSAK.name,
        fagsakId = "10695768",
        fagsaksystem = FAGSYSTEM,
    )
