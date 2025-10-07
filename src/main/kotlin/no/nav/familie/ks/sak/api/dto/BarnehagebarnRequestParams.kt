package no.nav.familie.ks.sak.api.dto

import org.springframework.data.domain.Sort

data class BarnehagebarnRequestParams(
    val ident: String?,
    val kommuneNavn: String?,
    val kunLøpendeAndel: Boolean,
    val limit: Int = 50,
    val offset: Int = 0,
    val sortBy: String = "kommuneNavn",
    val sortAsc: Boolean = false,
)

fun BarnehagebarnRequestParams.toSort() =
    if (sortAsc) {
        Sort.by(Sort.Order(Sort.Direction.ASC, getCorrectSortBy(sortBy), Sort.NullHandling.NULLS_LAST))
    } else {
        Sort.by(Sort.Order(Sort.Direction.DESC, getCorrectSortBy(sortBy), Sort.NullHandling.NULLS_LAST))
    }

private fun getCorrectSortBy(sortBy: String): String =
    when (sortBy.lowercase()) {
        "endrettidspunkt" -> "endretTid"
        "kommunenavn" -> "kommuneNavn"
        "kommunenr" -> "kommuneNr"
        "antalltimeribarnehage" -> "antallTimerBarnehage"
        else -> sortBy
    }
