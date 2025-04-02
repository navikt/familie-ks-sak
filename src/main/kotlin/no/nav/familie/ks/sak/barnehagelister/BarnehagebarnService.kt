package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.common.util.toPage
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BarnehagebarnService(
    val barnehagebarnRepository: BarnehagebarnRepository,
    val personidentRepository: PersonidentRepository,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    val fagsakRepository: FagsakRepository,
    val behandlingRepository: BehandlingRepository,
) {
    fun hentBarnehageBarn(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnVisningDto> {
        val barnehagebarn =
            when {
                !barnehagebarnRequestParams.ident.isNullOrEmpty() -> barnehagebarnRepository.findAllByIdent(barnehagebarnRequestParams.ident)
                !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() -> barnehagebarnRepository.findAllByKommuneNavn(barnehagebarnRequestParams.kommuneNavn)
                else -> barnehagebarnRepository.findAll()
            }

        val barnehagebarnDto =
            barnehagebarn
                .map { barn -> barn.tilBarnehageBarnDto() }

        val barnehagebarnDtoFiltrertPåLøpendeAndel =
            if (barnehagebarnRequestParams.kunLøpendeAndel) {
                barnehagebarnDto.filter { barn -> barn.fagsakId != null && erLøpendeAndelerPåBarnIAktivBehandling(barn.ident, barn.fagsakId) }
            } else {
                barnehagebarnDto
            }.sortedByDescending { it.endretTid } // Garanter at nyeste endretTid er det som blir beholdt
                .distinctBy { listOf(it.ident, it.fom, it.tom, it.antallTimerIBarnehage, it.endringstype, it.kommuneNavn, it.kommuneNr, it.fagsakId, it.fagsakstatus) }

        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, barnehagebarnRequestParams.toSort())
        return toPage(barnehagebarnDtoFiltrertPåLøpendeAndel, pageable, hentFeltSomSkalSorteresEtter(barnehagebarnRequestParams))
    }

    private fun hentFeltSomSkalSorteresEtter(barnehagebarnRequestParams: BarnehagebarnRequestParams): (BarnehagebarnVisningDto) -> Comparable<*>? {
        val feltStomSkalSorteresEtter: (BarnehagebarnVisningDto) -> Comparable<*>? =
            when (barnehagebarnRequestParams.sortBy) {
                "ident" -> { it -> it.ident }
                "endrettidspunkt" -> { it -> it.endretTid }
                "fom" -> { it -> it.fom }
                "tom" -> { it -> it.tom }
                "antalltimeribarnehage" -> { it -> it.antallTimerIBarnehage }
                "endringstype" -> { it -> it.endringstype }
                "kommunenavn" -> { it -> it.kommuneNavn }
                "kommunenr" -> { it -> it.kommuneNr }

                else -> { it -> it.endretTid }
            }
        return feltStomSkalSorteresEtter
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

    private fun Barnehagebarn.tilBarnehageBarnDto(): BarnehagebarnVisningDto {
        val fagsak = fagsakRepository.finnFagsakMedAktivBehandlingForIdent(ident)

        return BarnehagebarnVisningDto(
            ident = ident,
            fom = fom,
            tom = tom,
            antallTimerIBarnehage = antallTimerIBarnehage,
            endringstype = endringstype,
            kommuneNavn = kommuneNavn,
            kommuneNr = kommuneNr,
            fagsakId = fagsak?.id,
            fagsakstatus = fagsak?.status?.name,
            endretTid = endretTidspunkt,
        )
    }

    private fun erLøpendeAndelerPåBarnIAktivBehandling(
        barnsIdent: String,
        fagsakId: Long,
    ): Boolean {
        val aktør = personidentRepository.findByFødselsnummerOrNull(barnsIdent)?.aktør ?: return false
        val aktivBehandling = behandlingRepository.findByFagsakAndAktiv(fagsakId) ?: return false
        val andelTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(aktivBehandling.id, aktør)

        return andelTilkjentYtelse.any { it.erLøpende(YearMonth.now()) }
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
