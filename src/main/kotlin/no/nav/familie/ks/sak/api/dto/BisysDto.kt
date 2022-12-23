package no.nav.familie.ks.sak.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.ks.sak.common.exception.Feil
import org.springframework.http.HttpStatus
import java.time.YearMonth

data class BisysDto(val barnIdenter: List<String>) {
    init {
        if (barnIdenter.any { it.length != 11 }) {
            throw Feil("Ugyldig input. barnIdenter må være 11 siffer", httpStatus = HttpStatus.BAD_REQUEST)
        }
    }
}

data class BisysResponsDto(val utbetalingsinfo: Map<String, List<UtbetalingsinfoDto>>)

data class UtbetalingsinfoDto(
    @Schema(implementation = String::class, example = "2022-12")
    val fomMåned: YearMonth,
    @Schema(implementation = String::class, example = "2022-12")
    val tomMåned: YearMonth?,
    val beløp: Int
)
