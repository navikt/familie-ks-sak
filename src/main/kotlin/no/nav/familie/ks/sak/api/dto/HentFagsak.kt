package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import java.time.LocalDateTime
import java.util.UUID

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
    val tilbakekrevingsbehandlinger: List<TilbakekrevingsbehandlingResponsDto> = emptyList(),
    val gjeldendeUtbetalingsperioder: List<UtbetalingsperiodeResponsDto> = emptyList(),
    val klagebehandlinger: List<KlagebehandlingDto> = emptyList(),
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
)

data class TilbakekrevingsbehandlingResponsDto(
    val behandlingId: UUID,
    val opprettetTidspunkt: LocalDateTime,
    val aktiv: Boolean,
    val årsak: Behandlingsårsakstype?,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
    val resultat: Behandlingsresultatstype?,
    val vedtaksdato: LocalDateTime?,
)
