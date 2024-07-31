package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021

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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal
import java.time.LocalDate

fun List<VilkårResultat>.forskyvBarnehageplassVilkår(): List<Periode<VilkårResultat>> =
    tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder()
        .map {
            Periode(
                verdi = it.vilkårResultat,
                fom =
                    it.vilkårResultat.periodeFom
                        .tilForskøvetFomBasertPåGraderingsforskjell(it.graderingsforskjellMellomDenneOgForrigePeriode),
                tom =
                    it.vilkårResultat.periodeTom
                        .tilForskøvetTomBasertPåGraderingsforskjell(it.graderingsforskjellMellomDenneOgNestePeriode),
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
                vilkårResultat.hentGraderingsforskjellMellomDenneOgForrigePeriode(vilkårResultatIForrigePeriode)

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
                    graderingsforskjellMellomDenneOgNestePeriode = Graderingsforskjell.ReduksjonGårTilIngenUtbetaling,
                )
        }.filtrerBortNullverdier()
}

private fun VilkårResultat?.hentGraderingsforskjellMellomDenneOgForrigePeriode(
    vilkårResultatForrigePeriode: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?,
): Graderingsforskjell {
    val graderingForrigePeriode =
        vilkårResultatForrigePeriode?.vilkårResultat?.let { hentProsentForAntallTimer(vilkårResultatForrigePeriode.vilkårResultat.antallTimer) }
            ?: BigDecimal.ZERO
    val graderingDennePerioden = this?.let { hentProsentForAntallTimer(this.antallTimer) } ?: BigDecimal.ZERO
    val erFørstePeriode = vilkårResultatForrigePeriode == null

    val gikkPåBarnehageForrigePeriode = vilkårResultatForrigePeriode?.vilkårResultat?.antallTimer != null
    val gårIkkePåBarnehageDennePerioden = this?.antallTimer == null

    val sluttetIBarnehageDennePerioden = gikkPåBarnehageForrigePeriode && gårIkkePåBarnehageDennePerioden

    return when {
        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden == BigDecimal.ZERO -> Graderingsforskjell.ReduksjonGårTilIngenUtbetaling
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.Reduksjon

        sluttetIBarnehageDennePerioden -> Graderingsforskjell.ØkingGrunnetSluttIBarnehage
        graderingForrigePeriode < graderingDennePerioden && graderingForrigePeriode == BigDecimal.ZERO -> if (erFørstePeriode) Graderingsforskjell.IngenUtbetalingGrunnetFørsteperiodeTilØking else Graderingsforskjell.IngenUtbetalingGrunnetFullBarnehageplassTilØking

        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.Øking

        else -> Graderingsforskjell.Lik
    }
}

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMinstGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }
        .tilPerioderIkkeNull()

private fun LocalDate?.tilForskøvetTomBasertPåGraderingsforskjell(graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell) =
    this?.let { tomDato ->
        when (graderingsforskjellMellomDenneOgNestePeriode) {
            Graderingsforskjell.Lik,
            Graderingsforskjell.IngenUtbetalingGrunnetFullBarnehageplassTilØking,
            Graderingsforskjell.ØkingGrunnetSluttIBarnehage,
            Graderingsforskjell.Øking,
            -> tomDato.sisteDagIMåned()

            Graderingsforskjell.IngenUtbetalingGrunnetFørsteperiodeTilØking -> tomDato.plusDays(1).sisteDagIMåned()

            Graderingsforskjell.ReduksjonGårTilIngenUtbetaling,
            Graderingsforskjell.Reduksjon,
            -> tomDato.plusDays(1).minusMonths(1).sisteDagIMåned()
        }
    }

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør() = this?.tilForskøvetTomBasertPåGraderingsforskjell(Graderingsforskjell.ReduksjonGårTilIngenUtbetaling)?.toYearMonth()

private fun LocalDate?.tilForskøvetFomBasertPåGraderingsforskjell(graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell) =
    this?.let { fomDato ->
        when (graderingsforskjellMellomDenneOgForrigePeriode) {
            Graderingsforskjell.Lik,
            Graderingsforskjell.ØkingGrunnetSluttIBarnehage,
            Graderingsforskjell.Øking,
            -> fomDato.minusDays(1).plusMonths(1)?.førsteDagIInneværendeMåned()

            Graderingsforskjell.IngenUtbetalingGrunnetFullBarnehageplassTilØking,
            Graderingsforskjell.IngenUtbetalingGrunnetFørsteperiodeTilØking,
            ->
                fomDato
                    .plusMonths(1)
                    .førsteDagIInneværendeMåned()

            Graderingsforskjell.ReduksjonGårTilIngenUtbetaling,
            Graderingsforskjell.Reduksjon,
            -> fomDato.førsteDagIInneværendeMåned()
        }
    }
