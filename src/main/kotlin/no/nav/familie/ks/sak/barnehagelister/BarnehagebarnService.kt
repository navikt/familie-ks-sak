package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.common.util.saner
import no.nav.familie.ks.sak.common.util.toPage
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
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
    val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun hentBarnehageBarn(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnVisningDto> {
        val barnehagebarn =
            when {
                !barnehagebarnRequestParams.ident.isNullOrEmpty() -> barnehagebarnRepository.findAllByIdent(barnehagebarnRequestParams.ident.saner())
                !barnehagebarnRequestParams.kommuneNavn.isNullOrEmpty() -> barnehagebarnRepository.findAllByKommuneNavn(barnehagebarnRequestParams.kommuneNavn)
                else -> barnehagebarnRepository.findAll()
            }

        val barnehagebarnDto =
            barnehagebarn
                .mapNotNull { barn ->
                    val fagsak = fagsakRepository.finnFagsakMedAktivBehandlingForIdent(barn.ident)

                    val aktør = personidentRepository.findByFødselsnummerOrNull(barn.ident)?.aktør
                    val aktivBehandling = fagsak?.let { behandlingRepository.findByFagsakAndAktiv(fagsak.id) }
                    val andelTilkjentYtelse = if (aktør != null && aktivBehandling != null) andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(aktivBehandling.id, aktør) else emptyList()

                    val harLøpendeAndel = andelTilkjentYtelse.any { it.erLøpende(YearMonth.now()) }

                    if (barnehagebarnRequestParams.kunLøpendeAndel && !harLøpendeAndel) {
                        null
                    } else {
                        val vilkårsvurdering = aktivBehandling?.let { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(aktivBehandling.id) }
                        val personResultat = vilkårsvurdering?.personResultater?.find { it.aktør == aktør }
                        val vilkårResultat = personResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BARNEHAGEPLASS && it.periodeFom?.toYearMonth() == barn.fom.toYearMonth() }

                        val avvik = vilkårResultat?.antallTimer?.toDouble() != barn.antallTimerIBarnehage

                        BarnehagebarnVisningDto(
                            ident = barn.ident,
                            fom = barn.fom,
                            tom = barn.tom,
                            antallTimerBarnehage = barn.antallTimerIBarnehage,
                            endringstype = barn.endringstype,
                            kommuneNavn = barn.kommuneNavn,
                            kommuneNr = barn.kommuneNr,
                            fagsakId = fagsak?.id,
                            fagsakstatus = fagsak?.status?.name,
                            endretTid = barn.endretTidspunkt,
                            avvik = avvik,
                        )
                    }
                }.sortedByDescending { it.endretTid } // Garanter at nyeste endretTid er det som blir beholdt
                .distinctBy { listOf(it.ident, it.fom, it.tom, it.antallTimerBarnehage, it.endringstype, it.kommuneNavn, it.kommuneNr, it.fagsakId, it.fagsakstatus) }

        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, barnehagebarnRequestParams.toSort())
        return toPage(barnehagebarnDto, pageable, hentFeltSomSkalSorteresEtter(barnehagebarnRequestParams.sortBy))
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

    private fun BarnehagebarnRequestParams.toSort() =
        if (sortAsc) {
            Sort.by(sortBy).ascending()
        } else {
            Sort.by(sortBy).descending()
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
