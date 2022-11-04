package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrer
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammen
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.personident.Aktør

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vedtak: Vedtak,
    forskjøvetVilkårResultatTidslinjeMap: Map<Aktør, Tidslinje<List<VilkårResultat>>>
): List<VedtaksperiodeMedBegrunnelser> {

    val splittkriterierForVedtaksperiodeTidslinje = forskjøvetVilkårResultatTidslinjeMap
        .tilSplittkriterierForVedtaksperiodeTidslinjer().kombiner { it.toMap() }

    val andeltilkjentYtelserSplittetPåKriterier = andelerTilkjentYtelse
        .tilTidslinjerPerPerson().values
        .slåSammen()
        .filtrer { !it.isNullOrEmpty() }
        .kombinerMed(splittkriterierForVedtaksperiodeTidslinje) { andelerTilkjentYtelseIPeriode, splittkriterierPerPerson ->
            andelerTilkjentYtelseIPeriode?.let { Pair(andelerTilkjentYtelseIPeriode, splittkriterierPerPerson) }
        }

    return andeltilkjentYtelserSplittetPåKriterier
        .tilPerioderIkkeNull()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fom?.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING
            )
        }
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinjerPerPerson() =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        andelerTilkjentYtelsePåPerson.tilTidslinje()
    }

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje(): Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger> =
    this.map { Periode(it, it.stønadFom.førsteDagIInneværendeMåned(), it.stønadTom.sisteDagIInneværendeMåned()) }
        .tilTidslinje()

private data class SplittkriterierForVedtaksperiode(
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val regelverk: Regelverk?
)

private fun Map<Aktør, Tidslinje<List<VilkårResultat>>>.tilSplittkriterierForVedtaksperiodeTidslinjer():
    List<Tidslinje<Pair<Aktør, SplittkriterierForVedtaksperiode>>> =

    this.map { (aktør, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.tilPerioder().filtrerIkkeNull().map { vilkårResultater ->
            Periode(
                verdi = vilkårResultater.verdi.let {
                    val utdypendeVilkårsvurderinger = hentSetAvVilkårsVurderinger(it)
                    Pair(
                        aktør,
                        SplittkriterierForVedtaksperiode(
                            utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
                            regelverk = hentRegelverkPersonErVurdertEtterIPeriode(it)
                        )
                    )
                },
                fom = vilkårResultater.fom,
                tom = vilkårResultater.tom
            )
        }.tilTidslinje()
    }

private fun hentSetAvVilkårsVurderinger(vilkårResultater: List<VilkårResultat>) =
    vilkårResultater.flatMap { it.utdypendeVilkårsvurderinger }.toSet()

private fun hentRegelverkPersonErVurdertEtterIPeriode(vilkårResultater: Iterable<VilkårResultat>) =
    vilkårResultater
        .map { it.vurderesEtter }
        .reduce { acc, regelverk ->
            when {
                acc == null -> regelverk
                regelverk == null -> acc
                regelverk != acc -> throw Feil("Mer enn ett regelverk på person i periode: $regelverk, $acc")
                else -> acc
            }
        }
