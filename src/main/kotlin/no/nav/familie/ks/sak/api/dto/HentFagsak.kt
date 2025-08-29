package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import java.time.LocalDateTime

data class FagsakRequestDto(
    val personIdent: String?,
    val aktørId: String? = null,
)

data class MinimalFagsakResponsDto(
    val opprettetTidspunkt: LocalDateTime,
    val id: Long,
    val søkerFødselsnummer: String,
    val status: FagsakStatus,
    val underBehandling: Boolean,
    val løpendeKategori: BehandlingKategori?,
    val behandlinger: List<MinimalBehandlingResponsDto> = emptyList(),
    val gjeldendeUtbetalingsperioder: List<UtbetalingsperiodeResponsDto> = emptyList(),
)

data class MinimalBehandlingResponsDto(
    val behandlingId: Long,
    val opprettetTidspunkt: LocalDateTime,
    val aktivertTidspunkt: LocalDateTime,
    val kategori: BehandlingKategori,
    val aktiv: Boolean,
    val årsak: BehandlingÅrsak?,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val vedtaksdato: LocalDateTime?,
) {
    companion object {
        fun opprettFraBehandling(behandling: Behandling,
                vedtaksdato: LocalDateTime?,
        ) = MinimalBehandlingResponsDto(
            behandlingId = behandling.id,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            aktivertTidspunkt = behandling.aktivertTidspunkt,
            kategori = behandling.kategori,
            aktiv = behandling.aktiv,
            årsak = behandling.opprettetÅrsak,
            type = behandling.type,
            status = behandling.status,
            resultat = behandling.resultat,
            vedtaksdato = vedtaksdato,
        )
    }
}
