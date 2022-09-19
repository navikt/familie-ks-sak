package no.nav.familie.ks.sak.api.dto

import java.time.LocalDateTime

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
    val behandlinger: List<BehandlingResponsDto> = emptyList()
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
