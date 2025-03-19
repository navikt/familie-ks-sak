package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentstatus
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.kontrakter.felles.journalpost.Sak
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class JournalpostDokumentDto(
    val dokumentTittel: String?,
    val dokumentInfoId: String,
    val brevkode: String?,
    val logiskeVedlegg: List<LogiskVedlegg>?,
    val eksisterendeLogiskeVedlegg: List<LogiskVedlegg>?,
)

data class TilknyttetBehandling(
    val behandlingstype: JournalføringBehandlingstype,
    val behandlingId: String,
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
    val tilknyttedeBehandlinger: List<TilknyttetBehandling> = emptyList(),
    val dokumenter: List<JournalpostDokumentDto>,
    val navIdent: String,
    val nyBehandlingstype: JournalføringBehandlingstype,
    val nyBehandlingsårsak: BehandlingÅrsak,
    val journalførendeEnhet: String,
)

fun JournalføringRequestDto.tilOppdaterJournalpostRequestDto(
    sak: Sak,
    journalpost: Journalpost,
): OppdaterJournalpostRequestDto {
    val avsenderMottakerIdType =
        when {
            journalpost.kanal == "EESSI" -> journalpost.avsenderMottaker?.type
            this.avsender.id != "" -> AvsenderMottakerIdType.FNR
            else -> null
        }

    return OppdaterJournalpostRequestDto(
        avsenderMottaker =
            AvsenderMottaker(
                id = avsender.id,
                idType = avsenderMottakerIdType,
                navn = avsender.navn,
            ),
        bruker =
            JournalpostBrukerDto(
                id = bruker.id,
                navn = bruker.navn,
            ),
        sak = sak,
        tittel = journalpostTittel,
        dokumenter =
            dokumenter.map {
                DokumentInfo(
                    dokumentInfoId = it.dokumentInfoId,
                    tittel = it.dokumentTittel,
                    brevkode = it.brevkode,
                    dokumentstatus = Dokumentstatus.FERDIGSTILT,
                )
            },
    )
}

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
    FNR,
    ORGNR,
    AKTOERID,
}

enum class Sakstype(
    val type: String,
) {
    FAGSAK("FAGSAK"),
    GENERELL_SAK("GENERELL_SAK"),
}

sealed interface MottakerInfo {
    val navn: String
        get() = ""
    val manuellAdresseInfo: ManuellAdresseInfo?
        get() = null
}

class Bruker : MottakerInfo

class BrukerMedUtenlandskAdresse(
    override val manuellAdresseInfo: ManuellAdresseInfo,
) : MottakerInfo

class FullmektigEllerVerge(
    override val navn: String,
    override val manuellAdresseInfo: ManuellAdresseInfo,
) : MottakerInfo

class Dødsbo(
    override val navn: String,
    override val manuellAdresseInfo: ManuellAdresseInfo,
) : MottakerInfo

fun MottakerInfo.tilAvsenderMottaker(): AvsenderMottaker? =
    when (this) {
        is FullmektigEllerVerge, is Dødsbo ->
            AvsenderMottaker(
                navn = navn,
                id = null,
                idType = null,
            )
        // Trenger ikke overstyres når mottaker er bruker
        is Bruker, is BrukerMedUtenlandskAdresse -> null
    }
