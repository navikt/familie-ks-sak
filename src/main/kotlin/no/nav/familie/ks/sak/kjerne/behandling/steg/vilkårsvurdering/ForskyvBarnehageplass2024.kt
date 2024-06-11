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
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal
import java.time.LocalDate

fun List<VilkårResultat>.forskyvBarnehageplassVilkår2024(): List<Periode<VilkårResultat>> {
    return tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024()
        .map {
            Periode(
                verdi = it.vilkårResultat,
                fom =
                    it.vilkårResultat.periodeFom
                        .tilForskøvetFomBasertPåGraderingsforskjell2024(it.graderingsforskjellMellomDenneOgForrigePeriode),
                tom =
                    it.vilkårResultat.periodeTom
                        .tilForskøvetTomBasertPåGraderingsforskjell2024(it.graderingsforskjellMellomDenneOgNestePeriode),
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .filtrerBortOverlappendePerioderMedMinstGradering()
}

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<VilkårResultat>> {
    val vilkårResultatListeMedNullverdierForHullITidslinje: List<VilkårResultat?> =
        this
            .tilTidslinje()
            .tilPerioder()
            .map { it.verdi }

    return vilkårResultatListeMedNullverdierForHullITidslinje
        .fold(emptyList()) { acc: List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<VilkårResultat?>>, vilkårResultat ->
            val vilkårResultatIForrigePeriode = acc.lastOrNull()

            val graderingsforskjellMellomDenneOgForrigePeriode =
                vilkårResultat.hentGraderingsforskjellMellomDenneOgForrigePeriode2024(vilkårResultatIForrigePeriode)

            val accMedForrigeOppdatert =
                if (vilkårResultatIForrigePeriode == null) {
                    acc
                } else {
                    acc.dropLast(1) +
                        vilkårResultatIForrigePeriode
                            .copy(graderingsforskjellMellomDenneOgNestePeriode = graderingsforskjellMellomDenneOgForrigePeriode)
                }

            accMedForrigeOppdatert +
                BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024(
                    vilkårResultat = vilkårResultat,
                    graderingsforskjellMellomDenneOgForrigePeriode = graderingsforskjellMellomDenneOgForrigePeriode,
                    graderingsforskjellMellomDenneOgNestePeriode = Graderingsforskjell2024.ReduksjonGårTilIngenUtbetaling,
                )
        }.filtrerBortNullverdier()
}

private fun VilkårResultat?.hentGraderingsforskjellMellomDenneOgForrigePeriode2024(
    vilkårResultatForrigePeriode: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<VilkårResultat?>?,
): Graderingsforskjell2024 {
    val graderingForrigePeriode =
        vilkårResultatForrigePeriode?.vilkårResultat?.let { hentProsentForAntallTimer(vilkårResultatForrigePeriode.vilkårResultat.antallTimer) }
            ?: BigDecimal.ZERO
    val graderingDennePerioden = this?.let { hentProsentForAntallTimer(this.antallTimer) } ?: BigDecimal.ZERO
    val erFørstePeriode = vilkårResultatForrigePeriode == null

    val gikkPåBarnehageForrigePeriode = vilkårResultatForrigePeriode?.vilkårResultat?.antallTimer != null
    val gårIkkePåBarnehageDennePerioden = this?.antallTimer == null

    val sluttetIBarnehageDennePerioden = gikkPåBarnehageForrigePeriode && gårIkkePåBarnehageDennePerioden

    return when {
        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden == BigDecimal.ZERO -> Graderingsforskjell2024.ReduksjonGårTilIngenUtbetaling
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell2024.Reduksjon

        sluttetIBarnehageDennePerioden -> Graderingsforskjell2024.ØkingGrunnetSluttIBarnehage
        graderingForrigePeriode < graderingDennePerioden && graderingForrigePeriode == BigDecimal.ZERO -> if (erFørstePeriode) Graderingsforskjell2024.IngenUtbetalingGrunnetFørsteperiodeTilØking else Graderingsforskjell2024.IngenUtbetalingGrunnetFullBarnehageplassTilØking

        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell2024.Øking

        else -> Graderingsforskjell2024.Lik
    }
}

enum class Graderingsforskjell2024 {
    IngenUtbetalingGrunnetFullBarnehageplassTilØking,
    Øking,
    ØkingGrunnetSluttIBarnehage,
    ReduksjonGårTilIngenUtbetaling,
    Reduksjon,
    Lik,
    IngenUtbetalingGrunnetFørsteperiodeTilØking,
}

data class BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<NullableVilkårResultat : VilkårResultat?>(
    val vilkårResultat: NullableVilkårResultat,
    val graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell2024,
    val graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell2024,
)

@Suppress("UNCHECKED_CAST")
private fun List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<VilkårResultat?>>.filtrerBortNullverdier(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<VilkårResultat>> =
    this.filter { it.vilkårResultat != null } as List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024<VilkårResultat>>

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMinstGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }
        .tilPerioderIkkeNull()

private fun LocalDate?.tilForskøvetTomBasertPåGraderingsforskjell2024(graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell2024) =
    this?.let { tomDato ->
        when (graderingsforskjellMellomDenneOgNestePeriode) {
            Graderingsforskjell2024.Lik,
            Graderingsforskjell2024.IngenUtbetalingGrunnetFullBarnehageplassTilØking,
            Graderingsforskjell2024.ØkingGrunnetSluttIBarnehage,
            Graderingsforskjell2024.Øking,
            -> tomDato.minusMonths(1).sisteDagIMåned()

            Graderingsforskjell2024.ReduksjonGårTilIngenUtbetaling ->
                tomDato.plusDays(1).sisteDagIMåned()

            Graderingsforskjell2024.Reduksjon -> tomDato.plusDays(1).sisteDagIMåned()

            Graderingsforskjell2024.IngenUtbetalingGrunnetFørsteperiodeTilØking -> tomDato.plusDays(1).sisteDagIMåned()
        }
    }

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024() = this?.tilForskøvetTomBasertPåGraderingsforskjell2024(Graderingsforskjell2024.ReduksjonGårTilIngenUtbetaling)?.toYearMonth()

private fun LocalDate?.tilForskøvetFomBasertPåGraderingsforskjell2024(graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell2024) =
    this?.let { fomDato ->
        when (graderingsforskjellMellomDenneOgForrigePeriode) {
            Graderingsforskjell2024.Lik,
            Graderingsforskjell2024.ØkingGrunnetSluttIBarnehage,
            Graderingsforskjell2024.Øking,
            -> fomDato.førsteDagIInneværendeMåned()

            Graderingsforskjell2024.IngenUtbetalingGrunnetFullBarnehageplassTilØking,
            Graderingsforskjell2024.IngenUtbetalingGrunnetFørsteperiodeTilØking,
            ->
                fomDato.førsteDagIInneværendeMåned()

            Graderingsforskjell2024.ReduksjonGårTilIngenUtbetaling,
            Graderingsforskjell2024.Reduksjon,
            -> fomDato.plusMonths(1).førsteDagIInneværendeMåned()
        }
    }
