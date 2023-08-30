package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValuta
import java.time.LocalDate

data class FeilutbetaltValutaDto(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val feilutbetaltBeløp: Int,
)

fun FeilutbetaltValuta.tilFeilutbetaltValutaDto(): FeilutbetaltValutaDto =
    FeilutbetaltValutaDto(
        id = id,
        fom = fom,
        tom = tom,
        feilutbetaltBeløp = feilutbetaltBeløp,
    )
