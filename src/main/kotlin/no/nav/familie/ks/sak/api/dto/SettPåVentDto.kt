package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.settpåvent.domene.SettPåVent
import no.nav.familie.ks.sak.kjerne.settpåvent.domene.SettPåVentÅrsak
import java.time.LocalDate

data class SettPåVentDto(
    val frist: LocalDate,
    val årsak: SettPåVentÅrsak
)

fun SettPåVent.tilSettPåVentDto(): SettPåVentDto = SettPåVentDto(
    frist = this.frist,
    årsak = this.årsak
)
