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
    val løpendeKategori: String?
)
