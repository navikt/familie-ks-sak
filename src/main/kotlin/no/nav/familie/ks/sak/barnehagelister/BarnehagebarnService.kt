package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.api.dto.toSort
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnVisningDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
                ident = barnehagebarnRequestParams.ident.takeUnless { it.isNullOrBlank() },
                kommuneNavn = barnehagebarnRequestParams.kommuneNavn.takeUnless { it.isNullOrBlank() },
                pageable = pageable,
            )

        val barnehagebarnDto =
            barnehagebarn
                .map { barn ->
                    val fagsakIdTilFagsakStatus = fagsakRepository.finnFagsakIdOgStatusMedAktivBehandlingForIdent(barn.getIdent()).firstOrNull()
                    BarnehagebarnVisningDto.opprett(barn, fagsakIdTilFagsakStatus?.first, fagsakIdTilFagsakStatus?.second)
                }

        return barnehagebarnDto
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

    @Transactional
    fun finnKommunerSendtInnSisteDøgn(): Set<KommuneEllerBydel> {
        val barnSendtInnSisteDøgn =
            barnehagebarnRepository
                .findAll()
                .filter { it.endretTidspunkt >= LocalDateTime.now().minusDays(1) }
        return barnSendtInnSisteDøgn.map { KommuneEllerBydel(it.kommuneNr, it.kommuneNavn) }.toSet()
    }
}
