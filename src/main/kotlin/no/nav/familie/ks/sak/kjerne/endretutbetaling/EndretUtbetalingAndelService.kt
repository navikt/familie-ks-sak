package no.nav.familie.ks.sak.kjerne.endretutbetaling

import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.api.dto.SanityBegrunnelseMedEndringsårsakResponseDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerIngenOverlappendeEndring
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerPeriodeInnenforTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerTomDato
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerUtbetalingMotÅrsak
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelValidator.validerÅrsak
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.fraEndretUtbetalingAndelRequestDto
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
) {
    fun hentEndredeUtbetalingAndeler(behandlingId: Long) = endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandlingId)

    @Transactional
    fun oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling: Behandling,
        endretUtbetalingAndelRequestDto: EndretUtbetalingAndelRequestDto,
    ) {
        val endretUtbetalingAndel = endretUtbetalingAndelRepository.getReferenceById(endretUtbetalingAndelRequestDto.id)
        val vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id)
        val personopplysningGrunnlag =
            personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
        val person =
            personopplysningGrunnlag.personer.single { it.aktør.aktivFødselsnummer() == endretUtbetalingAndelRequestDto.personIdent }
        val andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        endretUtbetalingAndel.fraEndretUtbetalingAndelRequestDto(endretUtbetalingAndelRequestDto, person)

        val andreEndredeAndelerPåBehandling =
            hentEndredeUtbetalingAndeler(behandling.id)
                .filter { it.id != endretUtbetalingAndelRequestDto.id }

        val gyldigTomEtterDagensDato =
            beregnGyldigTomIFremtiden(
                andreEndredeAndelerPåBehandling = andreEndredeAndelerPåBehandling,
                endretUtbetalingAndel = endretUtbetalingAndel,
                andelTilkjentYtelser = andelTilkjentYtelser,
            )

        validerTomDato(
            tomDato = endretUtbetalingAndel.tom,
            gyldigTomEtterDagensDato = gyldigTomEtterDagensDato,
            årsak = endretUtbetalingAndel.årsak,
        )

        if (endretUtbetalingAndel.tom == null) {
            endretUtbetalingAndel.tom = gyldigTomEtterDagensDato
        }

        validerÅrsak(
            årsak = endretUtbetalingAndel.årsak,
            endretUtbetalingAndel = endretUtbetalingAndel,
            vilkårsvurdering = vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = behandling.id),
        )

        validerUtbetalingMotÅrsak(
            årsak = endretUtbetalingAndel.årsak,
            skalUtbetales = endretUtbetalingAndel.prosent != BigDecimal(0),
        )

        validerIngenOverlappendeEndring(
            endretUtbetalingAndel = endretUtbetalingAndel,
            eksisterendeEndringerPåBehandling = andreEndredeAndelerPåBehandling,
        )

        validerPeriodeInnenforTilkjentYtelse(endretUtbetalingAndel, andelTilkjentYtelser)

        endretUtbetalingAndelRepository.saveAndFlush(endretUtbetalingAndel)

        beregningService.oppdaterTilkjentYtelsePåBehandling(
            behandling,
            personopplysningGrunnlag,
            vilkårsvurdering,
            endretUtbetalingAndel,
        )

        endretUtbetalingAndelOppdatertAbonnementer.forEach { abonnent ->
            abonnent.tilpassKompetanserTilEndretUtbetalingAndeler(
                behandlingId = BehandlingId(behandling.id),
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
            abonnent.tilpassKompetanserTilEndretUtbetalingAndeler(
                behandlingId = BehandlingId(behandling.id),
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

    fun sanityBegrunnelseTilRestFormat(
        sanityBegrunnelser: List<SanityBegrunnelse>,
        begrunnelse: IBegrunnelse,
    ): List<SanityBegrunnelseMedEndringsårsakResponseDto> {
        val sanityBegrunnelse = begrunnelse.tilSanityBegrunnelse(sanityBegrunnelser)

        if (sanityBegrunnelse != null) {
            val visningsnavn = sanityBegrunnelse.navnISystem

            return listOf(
                SanityBegrunnelseMedEndringsårsakResponseDto(
                    id = begrunnelse,
                    navn = visningsnavn,
                    endringsårsaker = sanityBegrunnelse.endringsårsaker,
                ),
            )
        }

        return emptyList()
    }

    fun sanityBegrunnelserMedEndringsårsak(sanityBegrunnelser: List<SanityBegrunnelse>) =
        (NasjonalEllerFellesBegrunnelse.entries + EØSBegrunnelse.entries)
            .groupBy { it.begrunnelseType }
            .mapValues { begrunnelseGruppe ->
                begrunnelseGruppe.value
                    .flatMap { endretUtbetalingBegrunnelse ->
                        sanityBegrunnelseTilRestFormat(
                            sanityBegrunnelser,
                            endretUtbetalingBegrunnelse,
                        )
                    }
            }
}

interface EndretUtbetalingAndelerOppdatertAbonnent {
    fun tilpassKompetanserTilEndretUtbetalingAndeler(
        behandlingId: BehandlingId,
    )
}
