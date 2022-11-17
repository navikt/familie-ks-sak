package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal
import java.time.LocalDate

fun List<VilkårResultat>.forskyvBarnehageplassVilkår(): List<Periode<VilkårResultat>> {
    return tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder()
        .map {
            Periode(
                verdi = it.vilkårResultat,
                fom = it.vilkårResultat.periodeFom
                    .tilForskøvetFomBasertPåGraderingsforskjell(it.graderingsforskjellMellomDenneOgForrigePeriode),
                tom = it.vilkårResultat.periodeTom
                    .tilForskøvetTomBasertPåGraderingsforskjell(it.graderingsforskjellMellomDenneOgNestePeriode)
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .filtrerBortOverlappendePerioderMedMinstGradering()
}

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> {
    val vilkårResultatListeMedNullverdierForHullITidslinje: List<VilkårResultat?> = this
        .tilTidslinje()
        .tilPerioder()
        .map { it.verdi }

    return vilkårResultatListeMedNullverdierForHullITidslinje
        .fold(emptyList()) { acc: List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>, vilkårResultat ->
            val vilkårResultatIForrigePeriode = acc.lastOrNull()

            val graderingsforskjellMellomDenneOgForrigePeriode =
                vilkårResultat.hentGraderingsforskjellMellomDenneOgForrigePeriode(vilkårResultatIForrigePeriode)

            val accMedForrigeOppdatert =
                if (vilkårResultatIForrigePeriode == null) {
                    acc
                } else {
                    acc.dropLast(1) + vilkårResultatIForrigePeriode
                        .copy(graderingsforskjellMellomDenneOgNestePeriode = graderingsforskjellMellomDenneOgForrigePeriode)
                }

            accMedForrigeOppdatert + BarnehageplassVilkårMedGraderingsforskjellMellomPerioder(
                vilkårResultat = vilkårResultat,
                graderingsforskjellMellomDenneOgForrigePeriode = graderingsforskjellMellomDenneOgForrigePeriode,
                graderingsforskjellMellomDenneOgNestePeriode = Graderingsforskjell.Reduksjon
            )
        }.filtrerBortNullverdier()
}

private fun VilkårResultat?.hentGraderingsforskjellMellomDenneOgForrigePeriode(
    vilkårResultatForrigePeriode: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?
): Graderingsforskjell {
    val graderingForrigePeriode =
        vilkårResultatForrigePeriode?.vilkårResultat?.let { hentProsentForAntallTimer(vilkårResultatForrigePeriode.vilkårResultat.antallTimer) }
            ?: BigDecimal.ZERO
    val graderingDennePerioden = this?.let { hentProsentForAntallTimer(this.antallTimer) } ?: BigDecimal.ZERO

    return when {
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.Reduksjon
        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.Øking
        else -> Graderingsforskjell.Lik
    }
}

enum class Graderingsforskjell {
    Øking,
    Reduksjon,
    Lik
}

data class BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<NullableVilkårResultat : VilkårResultat?>(
    val vilkårResultat: NullableVilkårResultat,
    val graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell,
    val graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell
)

@Suppress("UNCHECKED_CAST")
private fun List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>.filtrerBortNullverdier():
    List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> =
    this.filter { it.vilkårResultat != null } as List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>>

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMinstGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }.tilPerioderIkkeNull()

private fun LocalDate?.tilForskøvetTomBasertPåGraderingsforskjell(
    graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell
) = if (graderingsforskjellMellomDenneOgNestePeriode == Graderingsforskjell.Reduksjon) {
    this?.plusDays(1)?.minusMonths(1)?.sisteDagIMåned()
} else this?.sisteDagIMåned()

private fun LocalDate?.tilForskøvetFomBasertPåGraderingsforskjell(
    graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell
) = if (graderingsforskjellMellomDenneOgForrigePeriode == Graderingsforskjell.Reduksjon) {
    this?.førsteDagIInneværendeMåned()
} else this?.minusDays(1)?.plusMonths(1)?.førsteDagIInneværendeMåned()
