package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnForListe
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class BarnehagebarnService(
    val barnehagebarnRepository: BarnehagebarnRepository,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    val fagsakRepository: FagsakRepository,
    val behandlingRepository: BehandlingRepository,
    val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun hentBarnehagebarnForVisning(barnehagebarnRequestParams: BarnehagebarnRequestParams): Page<BarnehagebarnVisningDto> {
        val sort = barnehagebarnRequestParams.toSort()
        val pageable = PageRequest.of(barnehagebarnRequestParams.offset, barnehagebarnRequestParams.limit, sort)

        val barnehagebarn =
            barnehagebarnRepository.finnBarnehagebarn(
                kunLøpendeAndeler = barnehagebarnRequestParams.kunLøpendeAndel,
                ident = barnehagebarnRequestParams.ident,
                kommuneNavn = barnehagebarnRequestParams.kommuneNavn,
                pageable = pageable,
            )

        val barnehagebarnDto =
            barnehagebarn
                .map { barn ->
                    mapBarnehagebarnTilBarnehagebarnDto(barn)
                }

        return barnehagebarnDto
    }

    private fun BarnehagebarnRequestParams.toSort() =
        if (sortAsc) {
            Sort.by(getCorrectSortBy(sortBy)).ascending()
        } else {
            Sort.by(getCorrectSortBy(sortBy)).descending()
        }

    private fun getCorrectSortBy(sortBy: String): String =
        when (sortBy.lowercase()) {
            "endrettidspunkt" -> "endretTid"
            "kommunenavn" -> "kommune_navn"
            "kommunenr" -> "kommune_nr"
            "antalltimeribarnehage" -> "antall_timer_i_barnehage"
            else -> sortBy
        }

    private fun mapBarnehagebarnTilBarnehagebarnDto(
        barnehagebarn: BarnehagebarnForListe,
    ): BarnehagebarnVisningDto {
        val fagsakIdTilFagsakStatus = fagsakRepository.finnFagsakIdOgStatusMedAktivBehandlingForIdent(barnehagebarn.getIdent()).firstOrNull()

        return BarnehagebarnVisningDto(
            ident = barnehagebarn.getIdent(),
            fom = barnehagebarn.getFom(),
            tom = barnehagebarn.getTom(),
            antallTimerBarnehage = barnehagebarn.getAntallTimerBarnehage(),
            endringstype = barnehagebarn.getEndringstype(),
            kommuneNavn = barnehagebarn.getKommuneNavn(),
            kommuneNr = barnehagebarn.getKommuneNr(),
            fagsakId = fagsakIdTilFagsakStatus?.first,
            fagsakstatus = fagsakIdTilFagsakStatus?.second.toString(),
            endretTid = barnehagebarn.getEndretTid(),
            avvik = barnehagebarn.getAvvik(),
        )
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
