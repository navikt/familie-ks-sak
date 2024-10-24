package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import java.math.BigDecimal
import java.time.YearMonth

fun forskyvBarnehageplassVilkår2024(
    alleVilkårResultat: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {
    val (
        barnehageplassVilkår,
        andreVilkår,
    ) = alleVilkårResultat.partition { it.vilkårType == Vilkår.BARNEHAGEPLASS }

    val tidligsteÅrMånedAlleAndreVilkårErOppfylt =
        utledTidligsteÅrMånedAlleAndreVilkårErOppfylt(
            andreVilkår,
        )

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
                finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
                    vilkårResultatIForrigePeriode?.vilkårResultat,
                    vilkårResultat,
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
