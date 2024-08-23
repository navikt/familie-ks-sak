package no.nav.familie.ks.sak.kjerne.avstemming.domene

import java.time.LocalDateTime
import java.util.UUID

data class GrensesnittavstemmingTaskDto(
    val fom: LocalDateTime,
    val tom: LocalDateTime,
    val avstemmingId: UUID?,
)
