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
        val hentForKunLøpendeFagsak = barnehagebarnRequestParams.kunLøpendeFagsak
        val dagensDato = LocalDate.now()

        return when {
            !barnehagebarnRequestParams.ident.isNullOrEmpty() ->
                hentBarnehageBarnMedIdent(hentForKunLøpendeFagsak, barnehagebarnRequestParams.ident, pageable)

            !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() ->
                hentBarnehageBarnMedKommuneNavn(hentForKunLøpendeFagsak, barnehagebarnRequestParams.kommuneNavn, pageable)

            else -> hentAlleBarnehageBarn(hentForKunLøpendeFagsak, pageable)
        }
    }

    fun hentBarnehagebarnInfotrygd(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnInfotrygdDto> {
        val sort = barnehagebarnRequestParams.toSort()
        val barna = infotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker()
        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, sort)
        val skalHaLøpendeFagsak = barnehagebarnRequestParams.kunLøpendeFagsak

        return when {
            !barnehagebarnRequestParams.ident.isNullOrEmpty() ->
                hentInfotrygdBarnehagebarnFraIdent(skalHaLøpendeFagsak, barnehagebarnRequestParams.ident, barna, pageable)

            !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() ->
                hentInfotrygdBarnehagebarnFraKommuneNavn(skalHaLøpendeFagsak, barnehagebarnRequestParams.kommuneNavn, barna, pageable)

            else -> hentAlleBarnehageBarnInfotrygd(skalHaLøpendeFagsak, barna, pageable)
        }
    }

    private fun hentAlleBarnehageBarn(
        hentForKunLøpendeFagsak: Boolean,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeFagsak) {
        barnehagebarnRepository.findBarnehagebarn(LØPENDE_FAGSAK_STATUS, pageable)
    } else {
        barnehagebarnRepository.findAlleBarnehagebarnUavhengigAvFagsak(pageable)
    }

    private fun hentAlleBarnehageBarnLøpendeAndel(
        hentForKunLøpendeAndel: Boolean,
        dagensDato: LocalDate,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeAndel) {
        barnehagebarnRepository.findBarnehagebarnLøpendeAndel(dagensDato, pageable)
    } else {
        barnehagebarnRepository.findAlleBarnehagebarnUavhengigAvFagsak(pageable)
    }

    private fun hentBarnehageBarnMedKommuneNavn(
        hentForKunLøpendeFagsak: Boolean,
        kommuneNavn: String,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeFagsak) {
        barnehagebarnRepository.findBarnehagebarnByKommuneNavn(LØPENDE_FAGSAK_STATUS, kommuneNavn, pageable)
    } else {
        barnehagebarnRepository.findBarnehagebarnByKommuneNavnUavhengigAvFagsak(kommuneNavn, pageable)
    }

    private fun hentBarnehageBarnMedIdent(
        hentForKunLøpendeFagsak: Boolean,
        ident: String,
        pageable: PageRequest,
    ) = if (hentForKunLøpendeFagsak) {
        barnehagebarnRepository.findBarnehagebarnByIdent(LØPENDE_FAGSAK_STATUS, ident, pageable)
    } else {
        barnehagebarnRepository.findBarnehagebarnByIdentUavhengigAvFagsak(ident, pageable)
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
        barna: List<String>,
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
        barna: List<String>,
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
        barna: List<String>,
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

    companion object {
        val LØPENDE_FAGSAK_STATUS = listOf("LØPENDE")
    }
}
