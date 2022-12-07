package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import java.time.LocalDate
import java.time.ZonedDateTime

data class SakStatistikkDto(
    val funksjonellTid: ZonedDateTime,
    val tekniskTid: ZonedDateTime,
    val opprettetDato: LocalDate,
    val funksjonellId: String,
    val sakId: String,
    val aktorId: Long,
    val bostedsland: String,
    val aktorer: List<AktørDVH>? = emptyList(),
    val sakStatus: String,
    val avsender: String,
    val ytelseType: String = "KONTANTSTOTTE"
)
