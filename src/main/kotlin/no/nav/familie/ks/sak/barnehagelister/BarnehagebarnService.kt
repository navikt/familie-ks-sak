package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BarnehagebarnService(
    val barnehagebarnRepository: BarnehagebarnRepository,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    val fagsakRepository: FagsakRepository,
    val behandlingRepository: BehandlingRepository,
    val vilkårsvurderingService: VilkårsvurderingService,
) {
    @Cacheable("barnehagebarn", cacheManager = "barnehagelisterCache")
    fun hentBarnehagebarnDtoer(
        kunLøpendeAndel: Boolean,
    ): List<BarnehagebarnVisningDto> {
        val barnehagebarn = barnehagebarnRepository.findAll()

        val barnehagebarnDto =
            barnehagebarn
                .map { barn ->
                    mapBarnehagebarnTilBarnehagebarnDto(barn)
                }.sortedByDescending { it.endretTid } // Garanter at nyeste endretTid er det som blir beholdt
                .distinctBy { listOf(it.ident, it.fom, it.tom, it.antallTimerBarnehage, it.endringstype, it.kommuneNavn, it.kommuneNr, it.fagsakId, it.fagsakstatus) }
        return barnehagebarnDto
    }

    private fun mapBarnehagebarnTilBarnehagebarnDto(
        barnehagebarn: Barnehagebarn,
    ): BarnehagebarnVisningDto {
        val fagsak = fagsakRepository.finnFagsakMedAktivBehandlingForIdent(barnehagebarn.ident)
        val andelTilkjentYtelse = andelTilkjentYtelseRepository.finnAktiveAndelerForIdent(barnehagebarn.ident)

        val harLøpendeAndel = andelTilkjentYtelse.any { it.erLøpende(YearMonth.now()) }
        return BarnehagebarnVisningDto(
            ident = barnehagebarn.ident,
            fom = barnehagebarn.fom,
            tom = barnehagebarn.tom,
            antallTimerBarnehage = barnehagebarn.antallTimerIBarnehage,
            endringstype = barnehagebarn.endringstype,
            kommuneNavn = barnehagebarn.kommuneNavn,
            kommuneNr = barnehagebarn.kommuneNr,
            fagsakId = fagsak?.id,
            fagsakstatus = fagsak?.status?.name,
            endretTid = barnehagebarn.endretTidspunkt,
            avvik = erAvvikPåBarn(andelTilkjentYtelse, barnehagebarn),
            løpendeAndel = harLøpendeAndel,
        )
    }

    private fun erAvvikPåBarn(
        andelTilkjentYtelse: List<AndelTilkjentYtelse>,
        barn: Barnehagebarn,
    ): Boolean {
        val aktivBehandlingId = andelTilkjentYtelse.map { it.behandlingId }.toSet().singleOrNull()
        val vilkårsvurdering = aktivBehandlingId?.let { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(aktivBehandlingId) }

        val aktør = andelTilkjentYtelse.map { it.aktør }.toSet().singleOrNull()
        val personResultat = vilkårsvurdering?.personResultater?.find { it.aktør == aktør }
        val vilkårResultat = personResultat?.vilkårResultater?.find { it.vilkårType == Vilkår.BARNEHAGEPLASS && it.periodeFom?.toYearMonth() == barn.fom.toYearMonth() }

        val avvik = vilkårResultat?.antallTimer?.toDouble() != barn.antallTimerIBarnehage
        return avvik
    }

    // Hvis barnehagebarnet tilhører en annen periode eller kommer fra en annen liste ansees det som en ny melding
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
