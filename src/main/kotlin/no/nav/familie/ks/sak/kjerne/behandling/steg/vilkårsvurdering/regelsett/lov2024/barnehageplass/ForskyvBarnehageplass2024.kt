package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.alleVilkårOppfyltEllerNull
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.forskyvAndreVilkår2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun forskyvBarnehageplassVilkår2024(
    vilkårResultat: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {
    val barnehageplassVilkår = vilkårResultat.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }
    val tidligsteÅrMånedAlleAndreVilkårErOppfylt =
        vilkårResultat
            .filter { it.vilkårType != Vilkår.BARNEHAGEPLASS }
            .groupBy { it.vilkårType }
            .map { forskyvAndreVilkår2024(it.key, it.value) }
            .map { it.tilTidslinje() }
            .kombiner {
                alleVilkårOppfyltEllerNull(
                    vilkårResultater = it,
                    personType = PersonType.BARN,
                    vilkårSomIkkeSkalSjekkesPå = listOf(Vilkår.BARNEHAGEPLASS),
                )
            }.tilPerioderIkkeNull()
            .mapNotNull { it.fom?.toYearMonth() }
            .minOfOrNull { it }

    return barnehageplassVilkår
        .tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024(tidligsteÅrMånedAlleAndreVilkårErOppfylt)
        .map {
            Periode(
                verdi = it.vilkårResultat,
                fom =
                    forskyvFomBasertPåGraderingsforskjell2024(
                        it.vilkårResultat.periodeFom,
                        it.graderingsforskjellMellomDenneOgForrigePeriode,
                    ),
                tom =
                    forskyvTomBasertPåGraderingsforskjell2024(
                        it.vilkårResultat.periodeTom,
                        it.graderingsforskjellMellomDenneOgNestePeriode,
                    ),
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .filtrerBortOverlappendePerioderMedMaksGradering()
}

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024() = forskyvTomBasertPåGraderingsforskjell2024(this, Graderingsforskjell.REDUKSJON)?.toYearMonth()

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024(
    tidligsteÅrMånedAlleAndreVilkårErOppfylt: YearMonth?,
): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> {
    val vilkårResultatListeMedNullverdierForHullITidslinje: List<VilkårResultat?> =
        this
            .tilTidslinje()
            .tilPerioder()
            .map { it.verdi }

    return vilkårResultatListeMedNullverdierForHullITidslinje
        .fold(emptyList()) { acc: List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>, vilkårResultat ->
            val vilkårResultatIForrigePeriode = acc.lastOrNull()

            val graderingsforskjellMellomDenneOgForrigePeriode =
                hentGraderingsforskjellMellomDenneOgForrigePeriode2024(
                    vilkårResultat,
                    vilkårResultatIForrigePeriode?.vilkårResultat?.antallTimer,
                    tidligsteÅrMånedAlleAndreVilkårErOppfylt,
                )

            val accMedForrigeOppdatert =
                if (vilkårResultatIForrigePeriode == null) {
                    acc
                } else {
                    acc.dropLast(1) +
                        vilkårResultatIForrigePeriode
                            .copy(graderingsforskjellMellomDenneOgNestePeriode = graderingsforskjellMellomDenneOgForrigePeriode)
                }

            accMedForrigeOppdatert +
                BarnehageplassVilkårMedGraderingsforskjellMellomPerioder(
                    vilkårResultat = vilkårResultat,
                    graderingsforskjellMellomDenneOgForrigePeriode = graderingsforskjellMellomDenneOgForrigePeriode,
                    graderingsforskjellMellomDenneOgNestePeriode = Graderingsforskjell.REDUKSJON,
                )
        }.filtrerBortNullverdier()
}

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMaksGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.minByOrNull { it.antallTimer ?: BigDecimal.ZERO } }
        .tilPerioderIkkeNull()
