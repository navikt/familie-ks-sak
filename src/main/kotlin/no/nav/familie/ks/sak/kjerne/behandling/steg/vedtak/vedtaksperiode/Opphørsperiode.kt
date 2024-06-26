package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import java.time.LocalDate
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Udefinert
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

data class Opphørsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
    val begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
) : Vedtaksperiode

fun mapTilOpphørsperioder(
    forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vilkårsvurdering: Vilkårsvurdering,
): List<Opphørsperiode> {
    val forrigeUtbetalingsperioder =
        if (forrigePersonopplysningGrunnlag != null) {
            mapTilUtbetalingsperioder(
                personopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                andelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            )
        } else {
            emptyList()
        }
    val utbetalingsperioder =
        mapTilUtbetalingsperioder(personopplysningGrunnlag, andelerTilkjentYtelse)

    val alleOpphørsperioder =
        if (forrigeUtbetalingsperioder.isNotEmpty() && utbetalingsperioder.isEmpty()) {
            listOf(
                Opphørsperiode(
                    periodeFom = forrigeUtbetalingsperioder.minOf { it.periodeFom },
                    periodeTom = forrigeUtbetalingsperioder.maxOf { it.periodeTom },
                ),
            )
        } else {
            if (utbetalingsperioder.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder),
                    finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder, vilkårsvurdering),
                ).flatten()
            }.sortedBy { it.periodeFom }
        }

    return slåSammenOpphørsperioder(alleOpphørsperioder)
}

fun slåSammenOpphørsperioder(alleOpphørsperioder: List<Opphørsperiode>): List<Opphørsperiode> {
    if (alleOpphørsperioder.isEmpty()) return emptyList()

    val sortertOpphørsperioder = alleOpphørsperioder.sortedBy { it.periodeFom }

    return sortertOpphørsperioder.fold(
        mutableListOf(
            sortertOpphørsperioder.first(),
        ),
    ) { acc: MutableList<Opphørsperiode>, nesteOpphørsperiode: Opphørsperiode ->
        val forrigeOpphørsperiode = acc.last()
        when {
            nesteOpphørsperiode.periodeFom.erSammeEllerFør(forrigeOpphørsperiode.periodeTom ?: TIDENES_ENDE) -> {
                acc[acc.lastIndex] =
                    forrigeOpphørsperiode.copy(
                        periodeTom =
                        maxOfOpphørsperiodeTom(
                            forrigeOpphørsperiode.periodeTom,
                            nesteOpphørsperiode.periodeTom,
                        ),
                    )
            }

            else -> {
                acc.add(nesteOpphørsperiode)
            }
        }

        acc
    }
}

private fun maxOfOpphørsperiodeTom(
    a: LocalDate?,
    b: LocalDate?,
): LocalDate? {
    return if (a != null && b != null) maxOf(a, b) else null
}

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(
    utbetalingsperioder: List<Utbetalingsperiode>,
    vilkårsvurdering: Vilkårsvurdering,
): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder.maxOf { it.periodeTom }.toYearMonth()
    val nesteMåned = inneværendeMåned().nesteMåned()

    val erFramtidigOpphørPgaBarnehageplass =
        vilkårsvurdering.personResultater.any { personResultat ->
            personResultat.vilkårResultater
                .filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.BARNEHAGEPLASS }
                .filter { it.periodeTom != null }
                .any { barnehageplassVilkårResultat ->
                    val periodeTom = barnehageplassVilkårResultat.periodeTom!!
                    val periodeTomForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør =
                        when (periodeTom.isBefore(DATO_LOVENDRING_2024)) {
                            true -> periodeTom.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør()
                            false -> periodeTom.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024()
                        }
                    barnehageplassVilkårResultat.søkerHarMeldtFraOmBarnehageplass == true &&
                            periodeTomForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør == sisteUtbetalingsperiodeTom
                }
        }

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned) || erFramtidigOpphørPgaBarnehageplass) {
        listOf(
            Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = null,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
                begrunnelser = if (erFramtidigOpphørPgaBarnehageplass) mutableListOf(NasjonalEllerFellesBegrunnelse.OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS) else mutableListOf(),
            ),
        )
    } else {
        emptyList()
    }
}

private fun finnOpphørsperioderMellomUtbetalingsperioder(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val utbetalingsperioderTidslinje =
        utbetalingsperioder.map { Periode(it, it.periodeFom, it.periodeTom) }.tilTidslinje()

    return utbetalingsperioderTidslinje.tilTidslinjePerioderMedDato().filter { it.periodeVerdi is Udefinert }
        .map {
            Opphørsperiode(
                periodeFom = it.fom.tilLocalDateEllerNull() ?: TIDENES_MORGEN,
                periodeTom = it.tom.tilLocalDateEllerNull(),
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
            )
        }
}

fun Opphørsperiode.tilVedtaksperiodeMedBegrunnelse(
    vedtak: Vedtak,
): VedtaksperiodeMedBegrunnelser =
    VedtaksperiodeMedBegrunnelser(
        fom = this.periodeFom,
        tom = this.periodeTom,
        vedtak = vedtak,
        type = this.vedtaksperiodetype,
    ).also { vedtaksperiodeMedBegrunnelser ->
        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            begrunnelser.map {
                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
            },
        )
    }
