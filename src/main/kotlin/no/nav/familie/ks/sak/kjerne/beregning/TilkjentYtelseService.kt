package no.nav.familie.ks.sak.kjerne.beregning

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
import no.nav.familie.ks.sak.kjerne.praksisendring.Praksisendring2024Service
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilkjentYtelseService(
    private val beregnAndelTilkjentYtelseService: BeregnAndelTilkjentYtelseService,
    private val overgangsordningAndelRepository: OvergangsordningAndelRepository,
    private val praksisendring2024Service: Praksisendring2024Service,
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
        val endretUtbetalingAndelerBarna = endretUtbetalingAndeler.filter { it.personer.any { person -> person.type == PersonType.BARN } }

        val andelerTilkjentYtelseBarnaUtenEndringer =
            beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, vilkårsvurdering, tilkjentYtelse)

        val andelerTilkjentYtelseBarnaMedAlleEndringer =
            AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer, endretUtbetalingAndeler = endretUtbetalingAndelerBarna, tilkjentYtelse = tilkjentYtelse)

        val overgangsordningAndelerSomAndelTilkjentYtelse =
            genererAndelerTilkjentYtelseFraOvergangsordningAndeler(
                behandlingId = vilkårsvurdering.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            )

        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        val alleOrdinæreAndeler = andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel }

        val alleOrdinæreAndelerJustert = alleOrdinæreAndeler.taHensynTilPraksisendringAndeler(andelerForPraksisendring2024)

        val alleAndelerTilkjentYtelse = alleOrdinæreAndelerJustert + overgangsordningAndelerSomAndelTilkjentYtelse

        tilkjentYtelse.andelerTilkjentYtelse.addAll(alleAndelerTilkjentYtelse)
        return tilkjentYtelse
    }

    private fun List<AndelTilkjentYtelse>.taHensynTilPraksisendringAndeler(praksisendringAndeler: List<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        val ordinæreAndeler = this

        if (praksisendringAndeler.isEmpty()) return ordinæreAndeler
        if (ordinæreAndeler.isEmpty()) return praksisendringAndeler

        val aktørerMedPraksisendringAndeler = praksisendringAndeler.map { it.aktør }
        val aktørerMedOrdinæreAndeler = ordinæreAndeler.map { it.aktør }

        val alleAktører = (aktørerMedPraksisendringAndeler + aktørerMedOrdinæreAndeler).distinct()

        val ordinæreAndelTidslinjer = ordinæreAndeler.tilSeparateTidslinjerForBarna()
        val praksisendringTidslinjer = praksisendringAndeler.tilSeparateTidslinjerForBarna()

        return alleAktører.flatMap { aktør ->
            val ordinæreAndelTidslinjeForAktør = ordinæreAndelTidslinjer[aktør] ?: return@flatMap praksisendringAndeler.filter { it.aktør == aktør }
            val praksisendringTidslinjeForAktør = praksisendringTidslinjer[aktør] ?: return@flatMap ordinæreAndeler.filter { it.aktør == aktør }

            ordinæreAndelTidslinjeForAktør
                .kombinerMed(praksisendringTidslinjeForAktør) { ordinærAndel, praksisendringAndel ->
                    praksisendringAndel ?: ordinærAndel
                }.tilAndelerTilkjentYtelse()
        }
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
