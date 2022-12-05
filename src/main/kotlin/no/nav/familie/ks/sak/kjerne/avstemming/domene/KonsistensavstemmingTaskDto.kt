package no.nav.familie.ks.sak.kjerne.avstemming.domene

import java.time.LocalDateTime
import java.util.UUID

data class KonsistensavstemmingTaskDto(
    val kjøreplanId: Long,
    val initieltKjøreTidspunkt: LocalDateTime,
    val transaksjonId: UUID = UUID.randomUUID(),
    val harSendtTilØkonomi: Boolean = true
)
