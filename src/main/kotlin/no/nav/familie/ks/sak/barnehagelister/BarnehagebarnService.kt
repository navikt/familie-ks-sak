package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnDtoInterface
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnInfotrygdDto
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BarnehagebarnService(
    private val infotrygdReplikaClient: InfotrygdReplikaClient,
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

    fun hentBarnehagebarnInfotrygd(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnInfotrygdDto> {
        val sort = barnehagebarnRequestParams.toSort()
        val barna = infotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker()
        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, sort)
        val skalHaLøpendeFagsak = barnehagebarnRequestParams.kunLøpendeAndel

        return when {
            !barnehagebarnRequestParams.ident.isNullOrEmpty() ->
                hentInfotrygdBarnehagebarnFraIdent(skalHaLøpendeFagsak, barnehagebarnRequestParams.ident, barna, pageable)

            !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() ->
                hentInfotrygdBarnehagebarnFraKommuneNavn(skalHaLøpendeFagsak, barnehagebarnRequestParams.kommuneNavn, barna, pageable)

            else -> hentAlleBarnehageBarnInfotrygd(skalHaLøpendeFagsak, barna, pageable)
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

    private fun hentInfotrygdBarnehagebarnFraKommuneNavn(
        skalHaLøpendeFagsak: Boolean,
        kommuneNavn: String,
        barna: Set<String>,
        pageable: PageRequest,
    ) = if (skalHaLøpendeFagsak) {
        barnehagebarnRepository
            .findBarnehagebarnByKommuneNavnInfotrygd(kommuneNavn, barna, pageable)
            .map { BarnehagebarnInfotrygdDto.fraBarnehageBarnInterfaceTilDto(it, true) }
    } else {
        barnehagebarnRepository
            .findBarnehagebarnByKommuneNavnInfotrygdUavhengigAvFagsak(
                kommuneNavn,
                pageable,
            ).map { BarnehagebarnInfotrygdDto.fraBarnehageBarnInterfaceTilDto(it, barna.contains(it.getIdent())) }
    }

    private fun hentInfotrygdBarnehagebarnFraIdent(
        skalHaLøpendeFagsak: Boolean,
        ident: String,
        barna: Set<String>,
        pageable: PageRequest,
    ) = if (skalHaLøpendeFagsak) {
        barnehagebarnRepository
            .findBarnehagebarnByIdentInfotrygd(ident, barna, pageable)
            .map { BarnehagebarnInfotrygdDto.fraBarnehageBarnInterfaceTilDto(it, true) }
    } else {
        barnehagebarnRepository
            .findBarnehagebarnByIdentInfotrygdUavhengigAvFagsak(ident, pageable)
            .map {
                BarnehagebarnInfotrygdDto.fraBarnehageBarnInterfaceTilDto(
                    it,
                    barna.contains(it.getIdent()),
                )
            }
    }

    private fun hentAlleBarnehageBarnInfotrygd(
        skalHaLøpendeFagsak: Boolean,
        barna: Set<String>,
        pageable: PageRequest,
    ) = if (skalHaLøpendeFagsak) {
        barnehagebarnRepository
            .findBarnehagebarnInfotrygd(barna, pageable)
            .map { BarnehagebarnInfotrygdDto.fraBarnehageBarnInterfaceTilDto(it, true) }
    } else {
        barnehagebarnRepository.findBarnehagebarnInfotrygdUavhengigAvFagsak(pageable).map {
            BarnehagebarnInfotrygdDto.fraBarnehageBarnInterfaceTilDto(
                it,
                barna.contains(it.getIdent()),
            )
        }
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
