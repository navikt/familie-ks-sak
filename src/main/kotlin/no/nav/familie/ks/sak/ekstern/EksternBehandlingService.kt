package no.nav.familie.ks.sak.ekstern

import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternBehandlingService(
    private val behandlingService: BehandlingService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val fagsakService: FagsakService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Transactional(readOnly = true)
    fun kanOppretteRevurdering(fagsakId: Long): KanOppretteRevurderingResponse {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> KanOppretteRevurderingResponse(true, null)
            is KanIkkeOppretteRevurdering -> KanOppretteRevurderingResponse(false, resultat.årsak.kanIkkeOppretteRevurderingÅrsak)
        }
    }

    @Transactional
    fun opprettRevurderingKlage(behandlingId: Long?): OpprettRevurderingResponse {
        if (behandlingId == null) return OpprettRevurderingResponse(IkkeOpprettet(IkkeOpprettetÅrsak.FEIL))

        val behandling = behandlingService.hentAktivtBehandling(behandlingId)

        val resultat = utledKanOppretteRevurdering(behandling.fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> opprettRevurdering(behandling)
            is KanIkkeOppretteRevurdering -> OpprettRevurderingResponse(IkkeOpprettet(resultat.årsak.ikkeOpprettetÅrsak))
        }
    }

    private fun opprettRevurdering(behandling: Behandling) = try {
        val behandlingDto = OpprettBehandlingDto(
            kategori = behandling.kategori,
            søkersIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.KLAGE
        )

        val revurdering = opprettBehandlingService.opprettBehandling(behandlingDto)
        OpprettRevurderingResponse(Opprettet(revurdering.id.toString()))
    } catch (e: Exception) {
        logger.error("Feilet opprettelse av revurdering for behandling=$behandling, se secure logg for detaljer")
        secureLogger.error("Feilet opprettelse av revurdering for behandling=$behandling", e)
        OpprettRevurderingResponse(IkkeOpprettet(IkkeOpprettetÅrsak.FEIL, e.message))
    }

    private fun utledKanOppretteRevurdering(fagsak: Fagsak): KanOppretteRevurderingResultat {
        val finnesÅpenBehandlingPåFagsak = behandlingService.erÅpenBehandlingPåFagsak(fagsak.id)
        if (finnesÅpenBehandlingPåFagsak) {
            return KanIkkeOppretteRevurdering(Årsak.ÅPEN_BEHANDLING)
        }
        if (!behandlingService.erAktivBehandlingPåFagsak(fagsak.id)) {
            return KanIkkeOppretteRevurdering(Årsak.INGEN_BEHANDLING)
        }
        return KanOppretteRevurdering
    }
}

private sealed interface KanOppretteRevurderingResultat
private object KanOppretteRevurdering : KanOppretteRevurderingResultat
private data class KanIkkeOppretteRevurdering(val årsak: Årsak) : KanOppretteRevurderingResultat

private enum class Årsak(
    val ikkeOpprettetÅrsak: IkkeOpprettetÅrsak,
    val kanIkkeOppretteRevurderingÅrsak: KanIkkeOppretteRevurderingÅrsak
) {

    ÅPEN_BEHANDLING(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING),
    INGEN_BEHANDLING(IkkeOpprettetÅrsak.INGEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING),
}
