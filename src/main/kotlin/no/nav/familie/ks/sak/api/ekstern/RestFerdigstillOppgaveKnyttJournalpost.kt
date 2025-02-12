import no.nav.familie.ks.sak.api.ekstern.RestJournalFøring
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class RestFerdigstillOppgaveKnyttJournalpost(
    val journalpostId: String,
    val tilknyttedeBehandlingIder: List<String> = emptyList(),
    val opprettOgKnyttTilNyBehandling: Boolean = false,
    val navIdent: String,
    val bruker: RestJournalFøring.NavnOgIdent,
    val nyBehandlingstype: BehandlingType,
    val nyBehandlingsårsak: BehandlingÅrsak,
    val kategori: BehandlingKategori?,
    val datoMottatt: LocalDateTime?,
)
