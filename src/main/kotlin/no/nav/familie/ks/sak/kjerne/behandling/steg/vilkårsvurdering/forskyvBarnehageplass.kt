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
            val fom = forskyvFomBasertPåGraderingsforskjell(it.vilkår.periodeFom, it.graderingsforskjellFraForrigePeriode)
            val tom = forskyvTomBasertPåGraderingsforskjell(it.graderingsforskjellTilNestePeriode, it.vilkår.periodeTom)

            Periode(
                verdi = it.vilkår,
                fom = fom,
                tom = tom
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .filtrerBortOverlappendePerioderMedMinstGradering()
}

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> {
    val oppfylteVilkårResultatListeMedNullverdierForHullITidslinje: List<VilkårResultat?> = this
        .tilTidslinje()
        .tilPerioder()
        .map { it.verdi }

    return oppfylteVilkårResultatListeMedNullverdierForHullITidslinje
        .fold(emptyList()) { acc: List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>, vilkårResultat ->
            val forrige = acc.lastOrNull()

            val graderingsforskjellFraForrigePeriode = hentGraderingsforskjellFraForrigePeriode(forrige, vilkårResultat)

            val accMedForrigeOppdatert =
                if (forrige == null) {
                    acc
                } else {
                    acc.dropLast(1) + forrige.copy(graderingsforskjellTilNestePeriode = graderingsforskjellFraForrigePeriode)
                }

            accMedForrigeOppdatert + BarnehageplassVilkårMedGraderingsforskjellMellomPerioder(
                vilkår = vilkårResultat,
                graderingsforskjellFraForrigePeriode = graderingsforskjellFraForrigePeriode,
                graderingsforskjellTilNestePeriode = Graderingsforskjell.Reduksjon
            )
        }.filtrerBortNullverdier()
}

private fun hentGraderingsforskjellFraForrigePeriode(
    forrige: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?,
    vilkårResultat: VilkårResultat?
): Graderingsforskjell {
    val graderingForrigePeriode = hentProsentForAntallTimer(forrige?.vilkår?.antallTimer)
    val graderingDennePerioden = hentProsentForAntallTimer(vilkårResultat?.antallTimer)

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
    val graderingsforskjellFraForrigePeriode: Graderingsforskjell,
    val graderingsforskjellTilNestePeriode: Graderingsforskjell
)

@Suppress("UNCHECKED_CAST")
private fun List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>.filtrerBortNullverdier():
    List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> =
    this.filter { it.vilkår != null } as List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>>

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMinstGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }.tilPerioderIkkeNull()

private fun forskyvTomBasertPåGraderingsforskjell(
    graderingsforskjellTilNestePeriode: Graderingsforskjell,
    periodeTom: LocalDate?
) = if (graderingsforskjellTilNestePeriode == Graderingsforskjell.Reduksjon) {
    periodeTom?.plusDays(1)?.minusMonths(1)?.sisteDagIMåned()
} else periodeTom?.plusDays(1)?.sisteDagIMåned()

private fun forskyvFomBasertPåGraderingsforskjell(
    periodeFom: LocalDate?,
    graderingsforskjellFraForrigePeriode: Graderingsforskjell
) = if (graderingsforskjellFraForrigePeriode == Graderingsforskjell.Reduksjon) {
    periodeFom?.førsteDagIInneværendeMåned()
} else periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned()
