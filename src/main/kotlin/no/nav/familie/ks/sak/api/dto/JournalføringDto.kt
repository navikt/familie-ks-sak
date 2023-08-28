package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class JournalpostDokumentDto(
    val dokumentTittel: String?,
    val dokumentInfoId: String,
    val brevkode: String?,
    val logiskeVedlegg: List<LogiskVedlegg>?,
    val eksisterendeLogiskeVedlegg: List<LogiskVedlegg>?,
)

data class JournalføringRequestDto(
    val avsender: NavnOgIdentDto,
    val bruker: NavnOgIdentDto,
    val datoMottatt: LocalDateTime?,
    val journalpostTittel: String?,
    val kategori: BehandlingKategori?,
    val knyttTilFagsak: Boolean,
    val opprettOgKnyttTilNyBehandling: Boolean,
    val tilknyttedeBehandlingIder: List<String>,
    val dokumenter: List<JournalpostDokumentDto>,
    val navIdent: String,
    val nyBehandlingstype: BehandlingType,
    val nyBehandlingsårsak: BehandlingÅrsak,
    val journalførendeEnhet: String,
)

fun JournalføringRequestDto.tilOppdaterJournalpostRequestDto(sak: Sak) =
    OppdaterJournalpostRequestDto(
        avsenderMottaker = AvsenderMottaker(
            id = avsender.id,
            idType = if (avsender.id.isNotBlank()) BrukerIdType.FNR else null,
            navn = avsender.navn,
        ),
        bruker = JournalpostBrukerDto(
            id = bruker.id,
            navn = bruker.navn,
        ),
        sak = sak,
        tittel = journalpostTittel,
        dokumenter = dokumenter.map {
            DokumentInfo(
                dokumentInfoId = it.dokumentInfoId,
                tittel = it.dokumentTittel,
                brevkode = it.brevkode,
                dokumentstatus = Dokumentstatus.FERDIGSTILT,
            )
        },
    )

data class NavnOgIdentDto(
    val navn: String,
    val id: String,
)

data class OppdaterJournalpostRequestDto(
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: JournalpostBrukerDto,
    val tema: Tema? = Tema.KON,
    val tittel: String? = null,
    val sak: Sak? = null,
    val dokumenter: List<DokumentInfo>? = null,
)

class JournalpostBrukerDto(
    val id: String,
    val idType: IdType? = IdType.FNR,
    val navn: String,
)

enum class IdType {
    FNR, ORGNR, AKTOERID
}

enum class Sakstype(val type: String) {
    FAGSAK("FAGSAK"),
    GENERELL_SAK("GENERELL_SAK"),
}
