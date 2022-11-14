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
                verdi = it.vilkår,
                fom = it.vilkår.periodeFom
                    .tilForskøvetFomBasertPåGraderingsforskjell(it.graderingsforskjellMellomDenneOgForrigePeriode),
                tom = it.vilkår.periodeTom
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
                vilkår = vilkårResultat,
                graderingsforskjellMellomDenneOgForrigePeriode = graderingsforskjellMellomDenneOgForrigePeriode,
                graderingsforskjellMellomDenneOgNestePeriode = Graderingsforskjell.Reduksjon
            )
        }.filtrerBortNullverdier()
}

private fun VilkårResultat?.hentGraderingsforskjellMellomDenneOgForrigePeriode(
    forrige: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?
): Graderingsforskjell {
    val graderingForrigePeriode = hentProsentForAntallTimer(forrige?.vilkår?.antallTimer)
    val graderingDennePerioden = hentProsentForAntallTimer(this?.antallTimer)

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
    val vilkår: NullableVilkårResultat,
    val graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell,
    val graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell
)

@Suppress("UNCHECKED_CAST")
private fun List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>.filtrerBortNullverdier():
    List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> =
    this.filter { it.vilkår != null } as List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>>

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMinstGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }.tilPerioderIkkeNull()

private fun LocalDate?.tilForskøvetTomBasertPåGraderingsforskjell(
    graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell
) = if (graderingsforskjellMellomDenneOgNestePeriode == Graderingsforskjell.Reduksjon) {
    this?.plusDays(1)?.minusMonths(1)?.sisteDagIMåned()
} else this?.plusDays(1)?.sisteDagIMåned()

private fun LocalDate?.tilForskøvetFomBasertPåGraderingsforskjell(
    graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell
) = if (graderingsforskjellMellomDenneOgForrigePeriode == Graderingsforskjell.Reduksjon) {
    this?.førsteDagIInneværendeMåned()
} else this?.plusMonths(1)?.førsteDagIInneværendeMåned()
