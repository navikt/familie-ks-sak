import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.map
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
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

    // TODO: HELP
    //  val splittkriterierForVedtaksperiodeTidslinje =
    //      forskjøvetVilkårResultatTidslinjeMap
    //          .tilSplittkriterierForVedtaksperiodeTidslinjer()
    //          .kombinerUtenNull { it.filterNotNull().toMap() }
    //          .filtrer { !it.isNullOrEmpty() }
    //          .slåSammenLike()
//
    //  return andelerTilkjentYtelse
    //      .tilTidslinjerPerPerson().values
    //      .kombinerUtenNull { it }
    //      .filtrer { !it?.toList().isNullOrEmpty() }
    //      .leftJoin(splittkriterierForVedtaksperiodeTidslinje) { andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode ->
    //          Pair(andelerTilkjentYtelseIPeriode, utdypendeVilkårIPeriode)
    //      }
    //      .filtrerIkkeNull()
    //      .perioder()
    //      .map {
    //          VedtaksperiodeMedBegrunnelser(
    //              fom = it.fraOgMed.tilYearMonthEllerNull()?.førsteDagIInneværendeMåned(),
    //              tom = it.tilOgMed.tilYearMonthEllerNull()?.sisteDagIInneværendeMåned(),
    //              vedtak = vedtak,
    //              type = Vedtaksperiodetype.UTBETALING
    //          )
    //      }
    return emptyList()
}

private data class SplittkriterierForVedtaksperiode(
    val utdypendeVilkårsvurderinger: Set<UtdypendeVilkårsvurdering>,
    val regelverk: Regelverk?
)

private fun Map<Aktør, Tidslinje<List<VilkårResultat>>>.tilSplittkriterierForVedtaksperiodeTidslinjer():
        List<Tidslinje<Pair<Aktør, SplittkriterierForVedtaksperiode>>> =

    this.map { (aktør, vilkårsvurderingTidslinje) ->
        vilkårsvurderingTidslinje.tilPerioder().map { vilkårResultater ->
            Periode(
                verdi = vilkårResultater.verdi?.let {
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
