package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.common.util.saner
import no.nav.familie.ks.sak.common.util.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class BarnehagebarnPagineringService(
    private val barnehagebarnService: BarnehagebarnService,
) {
    fun hentPaginerteBarnehageBarn(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnVisningDto> {
        val alleBarnehagebarn =
            barnehagebarnService.hentBarnehagebarnDtoer()

        val filtrertBarnehagebarn =
            when {
                !barnehagebarnRequestParams.ident.isNullOrEmpty() -> alleBarnehagebarn.filter { it.ident == barnehagebarnRequestParams.ident.saner() }
                !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() -> alleBarnehagebarn.filter { it.kommuneNavn == barnehagebarnRequestParams.kommuneNavn }
                else -> alleBarnehagebarn
            }.filter { !barnehagebarnRequestParams.kunLøpendeAndel || it.løpendeAndel }

        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, barnehagebarnRequestParams.toSort())
        return toPage(filtrertBarnehagebarn, pageable, hentFeltSomSkalSorteresEtter(barnehagebarnRequestParams.sortBy))
    }

    private fun BarnehagebarnRequestParams.toSort() =
        if (sortAsc) {
            Sort.by(sortBy).ascending()
        } else {
            Sort.by(sortBy).descending()
        }

    private fun hentFeltSomSkalSorteresEtter(sortBy: String): Comparator<BarnehagebarnVisningDto> {
        val feltStomSkalSorteresEtter: Comparator<BarnehagebarnVisningDto> =
            when (sortBy.lowercase()) {
                "ident" -> compareBy { it.ident }
                "endrettidspunkt" -> compareBy { it.endretTid }
                "fom" -> compareBy { it.fom }
                "tom" -> compareBy { it.tom }
                "antalltimeribarnehage" -> compareBy { it.antallTimerBarnehage }
                "avvik" -> compareBy { it.avvik }
                "endringstype" -> compareBy { it.endringstype }
                "kommunenavn" -> compareBy { it.kommuneNavn }
                "kommunenr" -> compareBy { it.kommuneNr }

                else -> compareBy { it -> it.endretTid }
            }
        return feltStomSkalSorteresEtter
    }
}
