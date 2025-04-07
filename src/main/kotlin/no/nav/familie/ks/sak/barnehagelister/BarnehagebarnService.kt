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
        return toPage(barnehagebarnDtoFiltrertPåLøpendeAndel, pageable, hentFeltSomSkalSorteresEtter(barnehagebarnRequestParams.sortBy))
    }

    private fun hentFeltSomSkalSorteresEtter(sortBy: String): Comparator<BarnehagebarnVisningDto> {
        val feltStomSkalSorteresEtter: Comparator<BarnehagebarnVisningDto> =
            when (sortBy) {
                "ident" -> compareBy { it.ident }
                "endrettidspunkt" -> compareBy { it.endretTid }
                "fom" -> compareBy { it -> it.fom }
                "tom" -> compareBy { it -> it.tom }
                "antalltimeribarnehage" -> compareBy { it -> it.antallTimerIBarnehage }
                "endringstype" -> compareBy { it -> it.endringstype }
                "kommunenavn" -> compareBy { it -> it.kommuneNavn }
                "kommunenr" -> compareBy { it -> it.kommuneNr }

                else -> compareBy { it -> it.endretTid }
            }
        return feltStomSkalSorteresEtter
    }

    private fun BarnehagebarnRequestParams.toSort() =
        if (sortAsc) {
            Sort.by(sortBy).ascending()
        } else {
            Sort.by(sortBy).descending()
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
