package no.nav.familie.ks.sak.kjerne.avstemming.domene

import java.time.LocalDateTime

data class GrensesnittavstemmingTaskDto(
    val fom: LocalDateTime,
    val tom: LocalDateTime,
)
