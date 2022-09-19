package no.nav.familie.ks.sak.api.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class FagsakRequestDto(
    val personIdent: String?,
    val aktørId: String? = null
)

data class MinimalFagsakResponsDto(
    val opprettetTidspunkt: LocalDateTime,
    val id: Long,
    val søkerFødselsnummer: String,
    val status: String,
    val underBehandling: Boolean,
    val løpendeKategori: String?,
    val behandlinger: List<BehandlingResponsDto> = emptyList(),
    val tilbakekrevingsbehandlinger: List<TilbakekrevingsbehandlingResponsDto> = emptyList(),
    val gjeldendeUtbetalingsperioder: List<UtbetalingsperiodeResponsDto> = emptyList()
)

data class BehandlingResponsDto(
    val behandlingId: Long,
    val opprettetTidspunkt: LocalDateTime,
    val kategori: String,
    val aktiv: Boolean,
    val årsak: String?,
    val type: String,
    val status: String,
    val resultat: String,
    val vedtaksdato: LocalDateTime?
)

data class TilbakekrevingsbehandlingResponsDto(
    val behandlingId: UUID,
    val opprettetTidspunkt: LocalDateTime,
    val aktiv: Boolean,
    val årsak: String?,
    val type: String,
    val status: String,
    val resultat: String?,
    val vedtaksdato: LocalDateTime?
)

data class UtbetalingsperiodeResponsDto(val periodeFom: LocalDate, val periodeTom: LocalDate)
