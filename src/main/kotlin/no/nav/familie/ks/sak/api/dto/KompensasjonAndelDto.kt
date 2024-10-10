package no.nav.familie.ks.sak.api.dto

import java.math.BigDecimal
import java.time.YearMonth

data class KompensasjonAndelDto(
    val id: Long,
    val personIdent: String?,
    val prosent: BigDecimal?,
    val fom: YearMonth?,
    val tom: YearMonth?,
)
