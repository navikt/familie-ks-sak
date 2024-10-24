package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.UtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.erObligatoriskeFelterUtfylt
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.fraOvergangsordningAndelDto
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilIOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilPerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OvergangsordningAndelService(
    private val overgangsordningAndelRepository: OvergangsordningAndelRepository,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun hentOvergangsordningAndeler(behandlingId: Long) = overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(behandlingId)

    @Transactional
    fun opprettTomOvergangsordningAndel(behandling: Behandling) = overgangsordningAndelRepository.save(OvergangsordningAndel(behandlingId = behandling.id))

    @Transactional
    fun oppdaterOvergangsordningAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        overgangsordningAndelId: Long,
        overgangsordningAndelRequestDto: OvergangsordningAndelDto,
    ) {
        val overgangsordningAndel = finnOvergangsordningAndel(overgangsordningAndelId)
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val person = personopplysningGrunnlag.personer.single { it.aktør.aktivFødselsnummer() == overgangsordningAndelRequestDto.personIdent }
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        overgangsordningAndel.fraOvergangsordningAndelDto(overgangsordningAndelRequestDto, person)

        // TODO: Validering

        slåSammenOgOppdaterOvergangsordningAndeler(behandling)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            vilkårsvurdering = vilkårsvurdering,
        )
    }

    @Transactional
    fun fjernOvergangsordningAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        overgangsordningAndelId: Long,
    ) {
        overgangsordningAndelRepository.deleteById(overgangsordningAndelId)

        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            vilkårsvurdering = vilkårsvurdering,
        )
    }

    @Transactional
    fun kopierOvergangsordningAndelFraForrigeBehandling(
        behandling: Behandling,
        forrigeBehandling: Behandling,
    ) = hentOvergangsordningAndeler(forrigeBehandling.id).map {
        overgangsordningAndelRepository.save(it.copy(id = 0, behandlingId = behandling.id))
    }

    private fun finnOvergangsordningAndel(overgangsordningAndelId: Long) =
        overgangsordningAndelRepository.finnOvergangsordningAndel(overgangsordningAndelId)
            ?: throw FunksjonellFeil(melding = "Fant ikke overgangsordningandel med id $overgangsordningAndelId")

    private fun slåSammenOgOppdaterOvergangsordningAndeler(behandling: Behandling) {
        val overgangsordningAndeler = hentOvergangsordningAndeler(behandling.id)
        val sammenslåtteOvergangsordningAndeler = overgangsordningAndeler.slåSammenLikePerioder()
        val utfyltePerioder = overgangsordningAndeler.filter { it.erObligatoriskeFelterUtfylt() }

        overgangsordningAndelRepository.deleteAll(utfyltePerioder)
        overgangsordningAndelRepository.saveAllAndFlush(sammenslåtteOvergangsordningAndeler)
    }

    private fun List<OvergangsordningAndel>.slåSammenLikePerioder(): List<OvergangsordningAndel> =
        utfyltePerioder()
            .tilPerioder()
            .tilTidslinje()
            .slåSammenLikePerioder()
            .tilPerioderIkkeNull()
            .map { it.verdi.tilOvergangsordningAndel(fom = it.fom!!.toYearMonth(), tom = it.tom!!.toYearMonth()) }
}
