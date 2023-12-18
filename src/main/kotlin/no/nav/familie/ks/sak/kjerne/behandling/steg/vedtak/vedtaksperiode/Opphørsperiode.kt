package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Udefinert
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate

data class Opphørsperiode(
    override val periodeFom: LocalDate,
    override val periodeTom: LocalDate?,
    override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
) : Vedtaksperiode

fun hentOpphørsperioder(
    behandling: Behandling,
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    personopplysningGrunnlagForrigeBehandling: PersonopplysningGrunnlag?,
    forrigeAndelerMedEndringer: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): List<Opphørsperiode> {
    if (behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET) return emptyList()

    return mapTilOpphørsperioder(
        forrigePersonopplysningGrunnlag = personopplysningGrunnlagForrigeBehandling,
        forrigeAndelerTilkjentYtelse = forrigeAndelerMedEndringer,
        personopplysningGrunnlag = personopplysningGrunnlag,
        andelerTilkjentYtelse = andelerTilkjentYtelse,
    )
}

fun mapTilOpphørsperioder(
    forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? = null,
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger> = emptyList(),
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
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
                    finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder),
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

private fun finnOpphørsperiodeEtterSisteUtbetalingsperiode(utbetalingsperioder: List<Utbetalingsperiode>): List<Opphørsperiode> {
    val sisteUtbetalingsperiodeTom = utbetalingsperioder.maxOf { it.periodeTom }.toYearMonth()
    val nesteMåned = inneværendeMåned().nesteMåned()

    return if (sisteUtbetalingsperiodeTom.isBefore(nesteMåned)) {
        listOf(
            Opphørsperiode(
                periodeFom = sisteUtbetalingsperiodeTom.nesteMåned().førsteDagIInneværendeMåned(),
                periodeTom = null,
                vedtaksperiodetype = Vedtaksperiodetype.OPPHØR,
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
