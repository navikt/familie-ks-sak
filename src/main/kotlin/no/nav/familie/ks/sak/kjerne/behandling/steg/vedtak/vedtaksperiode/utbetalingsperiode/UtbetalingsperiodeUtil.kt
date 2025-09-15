package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiode

import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammen
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal
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
    val splittkriterierForVilkårResultatTidslinje = forskjøvetVilkårResultatTidslinjeMap.tilSplittkriterierForVilkårTidslinje()
    val splittkriterierForKompetanseTidslinjer = kompetanser.tilSplittkriterierForKompetanseTidslinje()

    val andeltilkjentYtelserSplittetPåKriterier =
        andelerTilkjentYtelse
            .filter {
                !it.endreteUtbetalinger.any { endretUtbetalingAndel ->
                    endretUtbetalingAndel.årsak == Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024
                }
            }.tilTidslinjerPerPerson()
            .values
            .slåSammen()
            .filtrer { !it.isNullOrEmpty() }
            .kombinerMed(splittkriterierForVilkårResultatTidslinje, splittkriterierForKompetanseTidslinjer) {
                andelerTilkjentYtelseIPeriode,
                splittkriterierVilkår,
                splittKriterierKompetanse,
                ->
                andelerTilkjentYtelseIPeriode?.let {
                    SplittkriterierForVedtaksperiode(splittkriterierVilkår, splittKriterierKompetanse, andelerTilkjentYtelseIPeriode)
                }
            }

    return andeltilkjentYtelserSplittetPåKriterier
        .tilPerioderIkkeNull()
        .map { periode ->
            val vedtaksperiodeMedBegrunnelser =
                VedtaksperiodeMedBegrunnelser(
                    fom = periode.fom?.førsteDagIInneværendeMåned(),
                    tom = periode.tom?.sisteDagIMåned(),
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.UTBETALING,
                )
            if (vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.OVERGANGSORDNING_2024) {
                vedtaksperiodeMedBegrunnelser.begrunnelser.addAll(
                    hentBegrunnelserForOvergangsordningPerioder(periode)
                        .map { begrunnelse ->
                            begrunnelse.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
                        }.toMutableSet(),
                )
            }
            if (erFramtidigOpphørIForrigePeriode(forskjøvetVilkårResultatTidslinjeMap, periode)) {
                vedtaksperiodeMedBegrunnelser.begrunnelser.add(
                    NasjonalEllerFellesBegrunnelse.REDUKSJON_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser),
                )
            }
            vedtaksperiodeMedBegrunnelser
        }
}

fun erFramtidigOpphørIForrigePeriode(
    forskjøvetVilkårResultatTidslinjeMap: Map<Aktør, Tidslinje<List<VilkårResultat>>>,
    periode: Periode<SplittkriterierForVedtaksperiode>,
): Boolean {
    val vilkårsresultater =
        forskjøvetVilkårResultatTidslinjeMap
            .flatMap { (_, tidslinjePrPers) ->
                tidslinjePrPers.tilPerioder().flatMap { it.verdi ?: emptyList() }
            }
    return vilkårsresultater.any { it.periodeTom == periode.fom?.minusDays(1) && it.vilkårType == Vilkår.BARNEHAGEPLASS && it.søkerHarMeldtFraOmBarnehageplass == true }
}

fun hentBegrunnelserForOvergangsordningPerioder(periode: Periode<SplittkriterierForVedtaksperiode>): Set<NasjonalEllerFellesBegrunnelse> {
    val vedtaksperiodeFom = periode.fom
    val vedtaksperiodeTom = periode.tom

    if (vedtaksperiodeFom == null || vedtaksperiodeTom == null) return emptySet()

    val månedPeriodeForVedtaksperiode = MånedPeriode(vedtaksperiodeFom.toYearMonth(), vedtaksperiodeTom.toYearMonth())

    val overgangsordningAndelerIPeriode =
        periode.verdi.andelerTilkjentYtelse?.filter { andel ->
            val andelMånedPeriode = MånedPeriode(andel.stønadFom, andel.stønadTom)

            månedPeriodeForVedtaksperiode.overlapperHeltEllerDelvisMed(andelMånedPeriode) && andel.type == YtelseType.OVERGANGSORDNING
        } ?: emptyList()

    val overgangsordningBegrunnelser =
        overgangsordningAndelerIPeriode
            .filter { it.type == YtelseType.OVERGANGSORDNING }
            .map {
                when (it.prosent) {
                    BigDecimal(100) -> NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING
                    BigDecimal(50) -> NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_DELT_BOSTED
                    else -> NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING
                }
            }.distinct()
            .toSet()

    val skalGradertEllerDeltBostedBegrunnelseBrukes =
        overgangsordningBegrunnelser.contains(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_DELT_BOSTED) ||
            overgangsordningBegrunnelser.contains(NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING_GRADERT_UTBETALING)

    return if (skalGradertEllerDeltBostedBegrunnelseBrukes) {
        overgangsordningBegrunnelser.filter { it != NasjonalEllerFellesBegrunnelse.INNVILGET_OVERGANGSORDNING }.toSet()
    } else {
        overgangsordningBegrunnelser
    }
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinjerPerPerson() =
    groupBy { Pair(it.aktør, it.type) }.mapValues { (_, andelerTilkjentYtelsePåPerson) ->
        andelerTilkjentYtelsePåPerson.tilTidslinje()
    }

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilTidslinje(): Tidslinje<AndelTilkjentYtelseMedEndreteUtbetalinger> =
    this
        .map { Periode(it, it.stønadFom.førsteDagIInneværendeMåned(), it.stønadTom.sisteDagIInneværendeMåned()) }
        .tilTidslinje()

data class SplittkriterierForVilkår(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    val erEksplisittAvslagPåSøknad: Boolean,
    val regelverk: Regelverk?,
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
) {
    constructor(vilkårResultat: VilkårResultat) : this(
        vilkårType = vilkårResultat.vilkårType,
        resultat = vilkårResultat.resultat,
        erEksplisittAvslagPåSøknad = vilkårResultat.erEksplisittAvslagPåSøknad ?: false,
        regelverk = vilkårResultat.vurderesEtter,
        utdypendeVilkårsvurderinger =
            vilkårResultat.utdypendeVilkårsvurderinger
                .filter { it != UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD }
                .toSet(),
    )
}

private fun Map<Aktør, Tidslinje<List<VilkårResultat>>>.tilSplittkriterierForVilkårTidslinje(): Tidslinje<Map<Aktør, List<SplittkriterierForVilkår>>> =
    this
        .map { (aktør, vilkårsvurderingTidslinje) ->
            // Vi ønsker ikke å splitte opp vedtaksperiodene på grunn av splitt i barnets alder grunnet lovendringen i 2024 august.
            // Vurder å sjekke om personen er truffet av lovendringen i 2021 og 2024 først dersom det oppstår problemer
            val vilkårsvurderingTidslinjeUtenBarnetsAlder = vilkårsvurderingTidslinje.mapVerdi { it?.filter { vilkårResultat -> vilkårResultat.vilkårType != Vilkår.BARNETS_ALDER } }.slåSammenLikePerioder()

            vilkårsvurderingTidslinjeUtenBarnetsAlder
                .tilPerioder()
                .filtrerIkkeNull()
                .map { vilkårResultater ->
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
    constructor(kompetanse: Kompetanse) : this(
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

    return kompetanserPerBarn
        .map { (barn, kompetanse) ->
            kompetanse
                .map {
                    Periode(
                        barn to SplittkriterierForKompetanse(it),
                        it.fom?.førsteDagIInneværendeMåned(),
                        it.tom?.sisteDagIInneværendeMåned(),
                    )
                }.tilTidslinje()
        }.kombiner { it.toMap() }
}
