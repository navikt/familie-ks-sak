package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class ValutakursDto(
    val id: Long,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val barnIdenter: List<String>,
    val valutakursdato: LocalDate?,
    val valutakode: String?,
    val kurs: BigDecimal?,
    override val status: UtfyltStatus = UtfyltStatus.IKKE_UTFYLT,
) : AbstractEøsSkjemaUtfyltStatus<ValutakursDto>() {
    override fun medUtfyltStatus(): ValutakursDto = this.copy(status = utfyltStatus(finnAntallUtfylt(listOf(this.valutakursdato, this.kurs)), 2))
}

fun ValutakursDto.tilValutakurs(barnAktører: List<Aktør>) =
    Valutakurs(
        fom = this.fom,
        tom = this.tom,
        barnAktører = barnAktører.toSet(),
        valutakursdato = this.valutakursdato,
        valutakode = this.valutakode,
        kurs = this.kurs,
    )

fun Valutakurs.tilValutakursDto() =
    ValutakursDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        barnIdenter = this.barnAktører.map { it.aktivFødselsnummer() },
        valutakursdato = this.valutakursdato,
        valutakode = this.valutakode,
        kurs = this.kurs,
    ).medUtfyltStatus()
