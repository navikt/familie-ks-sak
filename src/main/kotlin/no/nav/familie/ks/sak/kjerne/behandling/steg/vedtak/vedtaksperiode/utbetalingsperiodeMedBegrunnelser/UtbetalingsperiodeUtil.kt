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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.LocalDate

// Om noe av dette endrer seg skal vi ha en splitt i vedtaksperiodene.
data class SplittkriterierForVedtaksperiode(
    val splittkriterierForVilkår: Map<Aktør, List<SplittkriterierForVilkår>>?,
    val splittkriterierForKompetanse: Map<Aktør, SplittkriterierForKompetanse>?,
    val andelerTilkjentYtelse: Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>?,
)

fun hentPerioderMedUtbetaling(
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    vedtak: Vedtak,
    forskjøvetVilkårResultatTidslinjeMap: Map<Aktør, Tidslinje<List<VilkårResultat>>>,
    kompetanser: List<Kompetanse>,
): List<VedtaksperiodeMedBegrunnelser> {
    val splittkriterierForVedtaksperiodeTidslinje = forskjøvetVilkårResultatTidslinjeMap.tilSplittkriterierForVilkårTidslinje()
    val splittkriterierForKompetanseTidslinjer = kompetanser.tilSplittkriterierForKompetanseTidslinje()

    val andeltilkjentYtelserSplittetPåKriterier =
        andelerTilkjentYtelse
            .tilTidslinjerPerPerson().values
            .slåSammen()
            .filtrer { !it.isNullOrEmpty() }
            .kombinerMed(splittkriterierForVedtaksperiodeTidslinje, splittkriterierForKompetanseTidslinjer) { andelerTilkjentYtelseIPeriode, splittkriterierVilkår, splittKriterierKompetanse ->
                andelerTilkjentYtelseIPeriode?.let {
                    SplittkriterierForVedtaksperiode(splittkriterierVilkår, splittKriterierKompetanse, andelerTilkjentYtelseIPeriode)
                }
            }

    return andeltilkjentYtelserSplittetPåKriterier
        .tilPerioderIkkeNull()
        .map {
            VedtaksperiodeMedBegrunnelser(
                fom = it.fom?.førsteDagIInneværendeMåned(),
                tom = it.tom?.sisteDagIMåned(),
                vedtak = vedtak,
                type = Vedtaksperiodetype.UTBETALING,
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

data class SplittkriterierForVilkår(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val erEksplisittAvslagPåSøknad: Boolean?,
    val regelverk: Regelverk?,
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
) {
    constructor(vilkårResultat: VilkårResultat) :
        this(
            vilkårType = vilkårResultat.vilkårType,
            resultat = vilkårResultat.resultat,
            periodeFom = vilkårResultat.periodeFom,
            periodeTom = vilkårResultat.periodeTom,
            erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad,
            regelverk = vilkårResultat.vurderesEtter,
            utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger.toSet(),
        )
}

private fun Map<Aktør, Tidslinje<List<VilkårResultat>>>.tilSplittkriterierForVilkårTidslinje(): Tidslinje<Map<Aktør, List<SplittkriterierForVilkår>>> =
    this.map { (aktør, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.tilPerioder().filtrerIkkeNull().map { vilkårResultater ->
            Periode(
                verdi = Pair(aktør, vilkårResultater.verdi.map { SplittkriterierForVilkår(it) }),
                fom = vilkårResultater.fom,
                tom = vilkårResultater.tom,
            )
        }.tilTidslinje()
    }.kombiner { it.toMap() }

data class SplittkriterierForKompetanse(
    val søkersAktivitet: KompetanseAktivitet? = null,
    val annenForeldersAktivitet: KompetanseAktivitet? = null,
    val annenForeldersAktivitetsland: String? = null,
    val søkersAktivitetsland: String? = null,
    val barnetsBostedsland: String? = null,
    val resultat: KompetanseResultat? = null,
    val erAnnenForelderOmfattetAvNorskLovgivning: Boolean? = false,
) {
    constructor(kompetanse: Kompetanse) :
        this(
            søkersAktivitet = kompetanse.søkersAktivitet,
            annenForeldersAktivitet = kompetanse.annenForeldersAktivitet,
            annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
            søkersAktivitetsland = kompetanse.søkersAktivitetsland,
            barnetsBostedsland = kompetanse.barnetsBostedsland,
            resultat = kompetanse.resultat,
            erAnnenForelderOmfattetAvNorskLovgivning = kompetanse.erAnnenForelderOmfattetAvNorskLovgivning,
        )
}

private fun List<Kompetanse>.tilSplittkriterierForKompetanseTidslinje(): Tidslinje<Map<Aktør, SplittkriterierForKompetanse>> {
    val alleBarna = this.flatMap { it.barnAktører }.toSet()

    val kompetanserPerBarn = alleBarna.associateWith { barn -> this.filter { barn in it.barnAktører } }

    return kompetanserPerBarn.map { (barn, kompetanse) ->
        kompetanse.map {
            Periode(
                barn to SplittkriterierForKompetanse(it),
                it.fom?.førsteDagIInneværendeMåned(),
                it.tom?.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()
    }.kombiner { it.toMap() }
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
