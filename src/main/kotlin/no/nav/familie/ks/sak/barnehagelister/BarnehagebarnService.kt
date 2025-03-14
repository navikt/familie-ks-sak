package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnDtoInterface
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BarnehagebarnService(
    private val barnehagebarnRepository: BarnehagebarnRepository,
) {
    fun hentBarnehageBarn(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnDtoInterface> {
        val sort = barnehagebarnRequestParams.toSort()
        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, sort)
        val hentForKunLøpendeAndel: Boolean = barnehagebarnRequestParams.kunLøpendeAndel
        val dagensDato = LocalDate.now()

        return when {
            !barnehagebarnRequestParams.ident.isNullOrEmpty() ->
                hentBarnehageBarnMedIdent(hentForKunLøpendeAndel, barnehagebarnRequestParams.ident, dagensDato, pageable)

            !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() ->
                hentBarnehageBarnMedKommuneNavn(hentForKunLøpendeAndel, barnehagebarnRequestParams.kommuneNavn, dagensDato, pageable)

            else ->
                hentAlleBarnehageBarn(hentForKunLøpendeAndel, dagensDato, pageable)
        }
    }

    private fun hentAlleBarnehageBarn(
        hentForKunLøpendeAndel: Boolean,
        dagensDato: LocalDate,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeAndel) {
        barnehagebarnRepository.findBarnehagebarn(dagensDato, pageable)
    } else {
        barnehagebarnRepository.findAlleBarnehagebarnUavhengigAvLøpendeAndel(pageable)
    }

    private fun hentBarnehageBarnMedKommuneNavn(
        hentForKunLøpendeAndel: Boolean,
        kommuneNavn: String,
        dagensDato: LocalDate,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeAndel) {
        barnehagebarnRepository.findBarnehagebarnByKommuneNavn(kommuneNavn, dagensDato, pageable)
    } else {
        barnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvLøpendeAndel(kommuneNavn, pageable)
    }

    private fun hentBarnehageBarnMedIdent(
        hentForKunLøpendeAndel: Boolean,
        ident: String,
        dagensDato: LocalDate,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeAndel) {
        barnehagebarnRepository.findBarnehagebarnByIdent(ident, dagensDato, pageable)
    } else {
        barnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvLøpendeAndel(ident, pageable)
    }

    private fun BarnehagebarnRequestParams.toSort() =
        if (sortAsc) {
            Sort.by(getCorrectSortBy(sortBy)).ascending()
        } else {
            Sort.by(getCorrectSortBy(sortBy)).descending()
        }

    private fun getCorrectSortBy(sortBy: String): String =
        when (sortBy.lowercase()) {
            "endrettidspunkt" -> "endret_tid"
            "kommunenavn" -> "kommune_navn"
            "kommunenr" -> "kommune_nr"
            "antalltimeribarnehage" -> "antall_timer_i_barnehage"
            else -> sortBy
        }

    // Hvis barnehagebarnet tilhører en annen periode eller kommer fra en annen liste ansees det som en ny melding, kunne muligens brukt barnehagebarn.id i stedet
    @Transactional
    fun erBarnehageBarnMottattTidligere(barnehagebarn: Barnehagebarn): Boolean =
        barnehagebarnRepository.findAllByIdent(barnehagebarn.ident).any { barnehageBarnMedSammeIdent ->
            barnehageBarnMedSammeIdent.fom == barnehagebarn.fom &&
                barnehageBarnMedSammeIdent.tom == barnehagebarn.tom &&
                barnehageBarnMedSammeIdent.arkivReferanse == barnehagebarn.arkivReferanse
        }

    @Transactional
    fun lagreBarnehageBarn(barnehagebarn: Barnehagebarn) {
        barnehagebarnRepository.saveAndFlush(barnehagebarn)
    }

    fun hentAlleKommuner(): Set<String> = barnehagebarnRepository.hentAlleKommuner()
}
