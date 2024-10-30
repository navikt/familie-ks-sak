package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig.Companion.OVERGANGSORDNING
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.utfyltePerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilkjentYtelseService(
    private val beregnAndelTilkjentYtelseService: BeregnAndelTilkjentYtelseService,
    private val overgangsordningAndelRepository: OvergangsordningAndelRepository,
    private val unleashService: UnleashService,
) {
    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
    ): TilkjentYtelse {
        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            )
        val endretUtbetalingAndelerBarna = endretUtbetalingAndeler.filter { it.person?.type == PersonType.BARN }

        val andelerTilkjentYtelseBarnaUtenEndringer =
            beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, vilkårsvurdering, tilkjentYtelse)

        val andelerTilkjentYtelseBarnaMedAlleEndringer =
            AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
            )

        val overgangsordningAndelerSomAndelTilkjentYtelse =
            if (unleashService.isEnabled(OVERGANGSORDNING)) {
                genererAndelerTilkjentYtelseFraOvergangsordningAndeler(
                    behandlingId = vilkårsvurdering.behandling.id,
                    tilkjentYtelse = tilkjentYtelse,
                )
            } else {
                emptyList()
            }

        val alleAndelerTilkjentYtelse = andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel } + overgangsordningAndelerSomAndelTilkjentYtelse

        tilkjentYtelse.andelerTilkjentYtelse.addAll(alleAndelerTilkjentYtelse)
        return tilkjentYtelse
    }

    private fun genererAndelerTilkjentYtelseFraOvergangsordningAndeler(
        behandlingId: Long,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> =
        overgangsordningAndelRepository
            .hentOvergangsordningAndelerForBehandling(behandlingId)
            .utfyltePerioder()
            .map { overgangsordningAndel ->
                val satsPeriode =
                    with(overgangsordningAndel) {
                        hentGyldigSatsFor(
                            antallTimer = antallTimer,
                            erDeltBosted = deltBosted,
                            stønadFom = fom,
                            stønadTom = tom,
                        )
                    }

                val utbetalingsbeløp = satsPeriode.sats.prosent(satsPeriode.prosent)

                AndelTilkjentYtelse(
                    behandlingId = behandlingId,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = overgangsordningAndel.person.aktør,
                    prosent = satsPeriode.prosent,
                    stønadFom = overgangsordningAndel.fom,
                    stønadTom = overgangsordningAndel.tom,
                    kalkulertUtbetalingsbeløp = utbetalingsbeløp,
                    nasjonaltPeriodebeløp = utbetalingsbeløp,
                    type = YtelseType.OVERGANGSORDNING,
                    sats = satsPeriode.sats,
                )
            }
}
