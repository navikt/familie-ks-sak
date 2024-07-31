package no.nav.familie.ks.sak.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.ks.sak.common.exception.Feil
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.YearMonth

data class BisysDto(
    val fom: LocalDate,
    val identer: List<String>,
) {
    init {
        if (identer.any { it.length != 11 }) {
            throw Feil("Ugyldig input. identerdenter må være 11 siffer", httpStatus = HttpStatus.BAD_REQUEST)
        }
    }
}

data class BisysResponsDto(
    val infotrygdPerioder: List<InfotrygdPeriode>,
    val ksSakPerioder: List<KsSakPeriode>,
)

data class InfotrygdPeriode(
    @Schema(implementation = String::class, example = "2022-12")
    val fomMåned: YearMonth,
    @Schema(implementation = String::class, example = "2022-12")
    val tomMåned: YearMonth?,
    val beløp: Int,
    val barna: List<String>,
)

data class KsSakPeriode(
    @Schema(implementation = String::class, example = "2022-12")
    val fomMåned: YearMonth,
    @Schema(implementation = String::class, example = "2022-12")
    val tomMåned: YearMonth?,
    val barn: Barn,
)

data class Barn(
    val beløp: Int,
    val ident: String,
)
