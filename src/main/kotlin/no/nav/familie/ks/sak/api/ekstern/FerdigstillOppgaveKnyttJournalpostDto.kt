import no.nav.familie.ks.sak.api.dto.TilknyttetBehandling
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.erAlfanummeriskPlussKolon
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.integrasjon.pdl.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class FerdigstillOppgaveKnyttJournalpostDto(
    val journalpostId: String,
    val tilknyttedeBehandlingIder: List<String> = emptyList(),
    val tilknyttedeBehandlinger: List<TilknyttetBehandling> = emptyList(),
    val opprettOgKnyttTilNyBehandling: Boolean = false,
    val navIdent: String? = null,
    val bruker: NavnOgIdent? = null,
    val nyBehandlingstype: JournalføringBehandlingstype? = null,
    val nyBehandlingsårsak: BehandlingÅrsak? = null,
    val kategori: BehandlingKategori? = null,
    val datoMottatt: LocalDateTime?,
    val fagsakId: Long? = null,
) {
    init {
        if (opprettOgKnyttTilNyBehandling && (nyBehandlingstype == null || nyBehandlingsårsak == null || navIdent == null || bruker == null || kategori == null)) {
            throw IllegalArgumentException("Må ha alle felter hvis opprettOgKnyttTilNyBehandling = true")
        }
    }
}

data class NavnOgIdent(
    val navn: String,
    val id: String,
) {
    // Bruker init til å validere personidenten
    init {
        if (!id.erAlfanummeriskPlussKolon()) {
            secureLogger.info("Ugyldig ident: $id")
            throw FunksjonellFeil(
                melding = "Ugyldig ident. Se securelog for mer informasjon.",
                frontendFeilmelding = "Ugyldig ident. Normalt et fødselsnummer eller organisasjonsnummer",
            )
        }
    }
}
