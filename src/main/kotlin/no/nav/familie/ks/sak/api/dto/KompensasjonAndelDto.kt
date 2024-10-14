package no.nav.familie.ks.sak.api.dto

import jakarta.validation.constraints.AssertTrue
import java.math.BigDecimal
import java.time.YearMonth

data class KompensasjonAndelDto(
    val id: Long,
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
) {
    @AssertTrue(message = "Til og med-dato kan ikke være før fra og med-dato")
    fun isTomSameOrAfterFom(): Boolean = fom == null || tom == null || !tom.isBefore(fom)

    @AssertTrue(message = "Personident må være elleve siffer")
    fun isPersonidentValid(): Boolean = personIdent == null || "\\d{11}".toRegex().matches(personIdent)

    @AssertTrue(message = "Prosent må være mellom 0 og 100")
    fun isProsentValid(): Boolean = prosent == null || prosent >= BigDecimal.ZERO && prosent <= BigDecimal(100)
}
