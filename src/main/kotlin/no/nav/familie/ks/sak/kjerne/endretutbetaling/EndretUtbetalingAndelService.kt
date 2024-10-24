package no.nav.familie.ks.sak.kjerne.endretutbetaling

import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerIngenOverlappendeEndring
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerTomDato
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerUtbetalingMotÅrsak
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerÅrsak
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class EndretUtbetalingAndelService(
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val beregningService: BeregningService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val endretUtbetalingAndelOppdatertAbonnementer: List<EndretUtbetalingAndelerOppdatertAbonnent> = emptyList(),
    private val endretUtbetalingAndelOppdaterer: EndretUtbetalingAndelOppdaterer,
) {
    fun hentEndredeUtbetalingAndeler(behandlingId: Long) = endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandlingId)

    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelRequestDto: EndretUtbetalingAndelRequestDto,
    ) {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val person =
            personopplysningGrunnlag.personer.single { it.aktør.aktivFødselsnummer() == endretUtbetalingAndelRequestDto.personIdent }
        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        val oppdatertEndretUtbetalingAndel =
            endretUtbetalingAndelOppdaterer.oppdaterEndretUtbetalingAndel(
                endretUtbetalingAndelRequestDto,
                person,
            )

        val andreEndredeAndelerPåBehandling =
            hentEndredeUtbetalingAndeler(behandling.id)
                .filter { it.id != endretUtbetalingAndelRequestDto.id }

        val gyldigTomEtterDagensDato =
            beregnGyldigTomIFremtiden(
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                endretUtbetalingAndel = oppdatertEndretUtbetalingAndel,
                andelTilkjentYtelser = andelTilkjentYtelser,
            )

        validerTomDato(
            tomDato = oppdatertEndretUtbetalingAndel.tom,
            gyldigTomEtterDagensDato = gyldigTomEtterDagensDato,
            årsak = oppdatertEndretUtbetalingAndel.årsak,
        )

        if (oppdatertEndretUtbetalingAndel.tom == null) {
            oppdatertEndretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }

        validerÅrsak(
            årsak = oppdatertEndretUtbetalingAndel.årsak,
            endretUtbetalingAndel = oppdatertEndretUtbetalingAndel,
            vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id),
        )

        validerUtbetalingMotÅrsak(
            årsak = oppdatertEndretUtbetalingAndel.årsak,
            skalUtbetales = oppdatertEndretUtbetalingAndel.prosent != BigDecimal(0),
        )

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = oppdatertEndretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling,
        )

        validerPeriodeInnenforTilkjentYtelse(oppdatertEndretUtbetalingAndel, andelTilkjentYtelser)

        endretUtbetalingAndelRepository.saveAndFlush(oppdatertEndretUtbetalingAndel)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling,
            personopplysningGrunnlag,
            vilkårsvurdering,
            oppdatertEndretUtbetalingAndel,
        )

        endretUtbetalingAndelOppdatertAbonnementer.forEach {
            it.endretUtbetalingAndelerOppdatert(
                behandlingId = behandling.id,
                endretUtbetalingAndeler = andreEndredeAndelerPåBehandling + oppdatertEndretUtbetalingAndel,
            )
        }
    }

    @Transactional
    fun fjernEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelId: Long,
    ) {
        endretUtbetalingAndelRepository.deleteById(endretUtbetalingAndelId)

        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandling.id)

        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling,
            personopplysningGrunnlag,
            vilkårsvurdering,
        )

        endretUtbetalingAndelOppdatertAbonnementer.forEach { abonnent ->
            abonnent.endretUtbetalingAndelerOppdatert(
                behandlingId = behandling.id,
                endretUtbetalingAndeler = endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandling.id),
            )
        }
    }

    @Transactional
    fun opprettTomEndretUtbetalingAndel(behandling: Behandling) =
        endretUtbetalingAndelRepository.save(EndretUtbetalingAndel(behandlingId = behandling.id))

    @Transactional
    fun kopierEndretUtbetalingAndelFraForrigeBehandling(
        behandling: Behandling,
        forrigeBehandling: Behandling,
    ) = hentEndredeUtbetalingAndeler(forrigeBehandling.id).forEach {
        val kopiertOverEndretUtbetalingAndel =
            it.copy(id = 0, behandlingId = behandling.id, erEksplisittAvslagPåSøknad = false)
        endretUtbetalingAndelRepository.save(kopiertOverEndretUtbetalingAndel)
    }
}

interface EndretUtbetalingAndelerOppdatertAbonnent {
    fun endretUtbetalingAndelerOppdatert(
        behandlingId: Long,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    )
}
