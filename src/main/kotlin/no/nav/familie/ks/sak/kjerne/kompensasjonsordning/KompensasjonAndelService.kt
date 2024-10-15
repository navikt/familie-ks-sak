package no.nav.familie.ks.sak.kjerne.kompensasjonsordning

import no.nav.familie.ks.sak.api.dto.KompensasjonAndelDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndelRepository
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.UtfyltKompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.erObligatoriskeFelterUtfylt
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.fraKompenasjonAndelDto
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.tilIKompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.tilKompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.tilPerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompensasjonAndelService(
    private val kompensasjonAndelRepository: KompensasjonAndelRepository,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
) {
    fun hentKompensasjonAndeler(behandlingId: Long) = kompensasjonAndelRepository.hentKompensasjonAndelerForBehandling(behandlingId)

    @Transactional
    fun opprettTomKompensasjonAndel(behandling: Behandling) = kompensasjonAndelRepository.save(KompensasjonAndel(behandlingId = behandling.id))

    @Transactional
    fun oppdaterKompensasjonAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        kompensasjonAndelId: Long,
        kompensasjonAndelRequestDto: KompensasjonAndelDto,
    ) {
        val kompensasjonAndel = finnKompensasjonAndel(kompensasjonAndelId)
        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val person = personopplysningGrunnlag.personer.single { it.aktør.aktivFødselsnummer() == kompensasjonAndelRequestDto.personIdent }
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        kompensasjonAndel.fraKompenasjonAndelDto(kompensasjonAndelRequestDto, person)

        // TODO: Validering

        slåSammenOgOppdaterKompensasjonAndeler(behandling)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling,
            personopplysningGrunnlag,
            vilkårsvurdering,
        )
    }

    @Transactional
    fun fjernKompensasjongAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        kompensasjonAndelId: Long,
    ) {
        kompensasjonAndelRepository.deleteById(kompensasjonAndelId)

        val personopplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling,
            personopplysningGrunnlag,
            vilkårsvurdering,
        )
    }

    @Transactional
    fun kopierKompensasjonAndelFraForrigeBehandling(
        behandling: Behandling,
        forrigeBehandling: Behandling,
    ) = hentKompensasjonAndeler(forrigeBehandling.id).map {
        kompensasjonAndelRepository.save(it.copy(id = 0, behandlingId = behandling.id))
    }

    private fun finnKompensasjonAndel(kompensasjonAndelId: Long) =
        kompensasjonAndelRepository.finnKompensasjonAndel(kompensasjonAndelId)
            ?: throw FunksjonellFeil(melding = "Fant ikke kompensasjonsandel med id $kompensasjonAndelId")

    private fun slåSammenOgOppdaterKompensasjonAndeler(behandling: Behandling) {
        val kompensasjonAndeler = hentKompensasjonAndeler(behandling.id)
        val sammenslåtteKompensasjonAndeler = kompensasjonAndeler.slåSammenLikePerioder()
        val utfyltePerioder = kompensasjonAndeler.filter { it.erObligatoriskeFelterUtfylt() }

        kompensasjonAndelRepository.deleteAll(utfyltePerioder)
        kompensasjonAndelRepository.saveAllAndFlush(sammenslåtteKompensasjonAndeler)
    }

    private fun List<KompensasjonAndel>.slåSammenLikePerioder(): List<KompensasjonAndel> =
        map { it.tilIKompensasjonAndel() }
            .filterIsInstance<UtfyltKompensasjonAndel>()
            .tilPerioder()
            .tilTidslinje()
            .slåSammenLikePerioder()
            .tilPerioderIkkeNull()
            .map { it.verdi.tilKompensasjonAndel(fom = it.fom!!.toYearMonth(), tom = it.tom!!.toYearMonth()) }
}
