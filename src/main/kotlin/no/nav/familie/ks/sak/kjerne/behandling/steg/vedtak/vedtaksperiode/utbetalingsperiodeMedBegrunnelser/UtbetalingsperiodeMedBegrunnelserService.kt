package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser

import hentPerioderMedUtbetaling
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class UtbetalingsperiodeMedBegrunnelserService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {
    fun hentUtbetalingsperioder(
        vedtak: Vedtak,
        opphørsperioder: List<VedtaksperiodeMedBegrunnelser>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val andelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(vedtak.behandling.id)

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandlingId = vedtak.behandling.id)

        val forskjøvetVilkårResultatTidslinjeMap =
            lagFørskjøvetVilkårResultatTidslinjeMap(vilkårsvurdering.personResultater)

        val utbetalingsperioder = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            vedtak = vedtak,
            forskjøvetVilkårResultatTidslinjeMap = forskjøvetVilkårResultatTidslinjeMap
        )

        return utbetalingsperioder
    }

    fun lagFørskjøvetVilkårResultatTidslinjeMap(personResultater: Set<PersonResultat>): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
        personResultater.associate { personResultat ->
            val vilkårResultaterForAktørMap = personResultat.vilkårResultater
                .groupByTo(mutableMapOf()) { it.vilkårType }
                .mapValues { if (it.key != Vilkår.BOR_MED_SØKER) it.value else it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() }

            val alleVilkår = vilkårResultaterForAktørMap.keys

            val vilkårResultaterKombinert = vilkårResultaterForAktørMap
                .tilVilkårResultatTidslinjer()
                .kombiner { alleVilkårOppfyltEllerNull(it, alleVilkår) }
                .tilPerioder()
                .filtrerIkkeNull()
                .map {
                    Periode(
                        it.verdi,
                        it.fom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
                        it.tom?.minusMonths(1)?.sisteDagIMåned()
                    )
                }
                .tilTidslinje()

            // TODO: Forflytt riktig når vi kommer fram til riktig regelverk

            Pair(
                personResultat.aktør,
                vilkårResultaterKombinert
            )
        }

    private fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
        if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

    private fun Map<Vilkår, List<VilkårResultat>>.tilVilkårResultatTidslinjer() =
        this.map { (_, vilkårResultater) ->
            vilkårResultater.map { Periode(it, it.periodeFom, it.periodeTom) }.tilTidslinje()
        }

    private fun alleVilkårOppfyltEllerNull(
        vilkårResultater: Iterable<VilkårResultat?>,
        vilkårForPerson: Set<Vilkår>
    ): List<VilkårResultat>? =
        if (erAlleVilkårForPersonOppfylt(vilkårForPerson, vilkårResultater))
            vilkårResultater.filterNotNull()
        else null

    private fun erAlleVilkårForPersonOppfylt(
        vilkårForPerson: Set<Vilkår>,
        vilkårResultater: Iterable<VilkårResultat?>
    ) =
        vilkårForPerson.all { vilkår -> vilkårResultater.any { it?.resultat == Resultat.OPPFYLT && it.vilkårType == vilkår } }
}
