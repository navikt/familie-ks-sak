package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass

import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.math.BigDecimal

fun forskyvBarnehageplassVilkår(
    vilkårResultats: List<VilkårResultat>,
): List<Periode<VilkårResultat>> =
    vilkårResultats
        .tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder()
        .map {
            Periode(
                verdi = it.vilkårResultat,
                fom =
                    forskyvFomBasertPåGraderingsforskjell(
                        it.vilkårResultat.periodeFom,
                        it.graderingsforskjellMellomDenneOgForrigePeriode,
                    ),
                tom =
                    forskyvTomBasertPåGraderingsforskjell(
                        it.vilkårResultat.periodeTom,
                        it.graderingsforskjellMellomDenneOgNestePeriode,
                    ),
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .filtrerBortOverlappendePerioderMedMinstGradering()

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> {
    val vilkårResultatListeMedNullverdierForHullITidslinje: List<VilkårResultat?> =
        this
            .tilTidslinje()
            .tilPerioder()
            .map { it.verdi }

    return vilkårResultatListeMedNullverdierForHullITidslinje
        .fold(emptyList()) { acc: List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>, vilkårResultat ->
            val vilkårResultatIForrigePeriode = acc.lastOrNull()

            val graderingsforskjellMellomDenneOgForrigePeriode =
                finnGraderingsforskjellMellomDenneOgForrigePeriode(
                    vilkårResultatForrigePeriode = vilkårResultatIForrigePeriode?.vilkårResultat,
                    vilkårResultatDennePerioden = vilkårResultat,
                    erFørstePeriode = vilkårResultatIForrigePeriode == null,
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
                    graderingsforskjellMellomDenneOgNestePeriode = Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
                )
        }.filtrerBortNullverdier()
}

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMinstGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }
        .tilPerioderIkkeNull()
