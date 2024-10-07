package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal
import java.time.LocalDate

fun List<VilkårResultat>.forskyvBarnehageplassVilkår2024(): List<Periode<VilkårResultat>> =
    tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024()
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
        .filtrerBortOverlappendePerioderMedMaksGradering()

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024() = this?.tilForskøvetTomBasertPåGraderingsforskjell2024(Graderingsforskjell.REDUKSJON)?.toYearMonth()

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedGraderingsforskjellMellomPerioder2024(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> {
    val vilkårResultatListeMedNullverdierForHullITidslinje: List<VilkårResultat?> =
        this
            .tilTidslinje()
            .tilPerioder()
            .map { it.verdi }

    return vilkårResultatListeMedNullverdierForHullITidslinje
        .foldIndexed(emptyList()) { index, acc: List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>, vilkårResultat ->
            val vilkårResultatIForrigePeriode = acc.lastOrNull()



            val graderingsforskjellMellomDenneOgForrigePeriode =
                vilkårResultat.hentGraderingsforskjellMellomDenneOgForrigePeriode2024(
                    erFørsteVilkårsperiode = index == 0,
                    vilkårResultatForrigePeriode = vilkårResultatIForrigePeriode,
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

private fun VilkårResultat?.hentGraderingsforskjellMellomDenneOgForrigePeriode2024(
    erFørsteVilkårsperiode: Boolean,
    vilkårResultatForrigePeriode: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?,
): Graderingsforskjell {
    val graderingForrigePeriode =
        vilkårResultatForrigePeriode?.vilkårResultat?.let { hentProsentForAntallTimer(vilkårResultatForrigePeriode.vilkårResultat.antallTimer) }
            ?: BigDecimal.ZERO
    val graderingDennePerioden = this?.let { hentProsentForAntallTimer(this.antallTimer) } ?: BigDecimal.ZERO
    return when {
        graderingDennePerioden == BigDecimal.ZERO && graderingForrigePeriode == BigDecimal(100) -> Graderingsforskjell.REDUKSJON_FRA_INGEN_BARNEHAGEPLASS_TIL_FULLTID_BARNEHAGEPLASS
        !erFørsteVilkårsperiode && graderingForrigePeriode == BigDecimal.ZERO && graderingDennePerioden == BigDecimal(100) -> Graderingsforskjell.ØKNING_FRA_FULLTID_BARNEHAGEPLASS_TIL_INGEN_BARNEHAGEPLASS
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.REDUKSJON
        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.ØKNING
        else -> Graderingsforskjell.LIK
    }
}

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioderMedMaksGradering() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.minByOrNull { it.antallTimer ?: BigDecimal.ZERO } }
        .tilPerioderIkkeNull()

private fun LocalDate?.tilForskøvetTomBasertPåGraderingsforskjell2024(graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell) =
    this?.let { tomDato ->
        when (graderingsforskjellMellomDenneOgNestePeriode) {
            Graderingsforskjell.LIK,
            -> tomDato.minusMonths(1).sisteDagIMåned()

            Graderingsforskjell.ØKNING,
            -> tomDato.sisteDagIMåned()
            Graderingsforskjell.ØKNING_FRA_FULLTID_BARNEHAGEPLASS_TIL_INGEN_BARNEHAGEPLASS,
            -> tomDato.sisteDagIMåned()

            Graderingsforskjell.REDUKSJON,
            -> tomDato.plusDays(1).sisteDagIMåned()

            Graderingsforskjell.REDUKSJON_FRA_INGEN_BARNEHAGEPLASS_TIL_FULLTID_BARNEHAGEPLASS,
            -> tomDato.plusDays(1).minusMonths(1).sisteDagIMåned()
        }
    }

private fun LocalDate?.tilForskøvetFomBasertPåGraderingsforskjell2024(graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell) =
    this?.let { fomDato ->
        when (graderingsforskjellMellomDenneOgForrigePeriode) {
            Graderingsforskjell.LIK,
            Graderingsforskjell.ØKNING,
            -> fomDato.førsteDagIInneværendeMåned()

            Graderingsforskjell.REDUKSJON,
            Graderingsforskjell.REDUKSJON_FRA_INGEN_BARNEHAGEPLASS_TIL_FULLTID_BARNEHAGEPLASS,
            -> fomDato.førsteDagINesteMåned()
            Graderingsforskjell.ØKNING_FRA_FULLTID_BARNEHAGEPLASS_TIL_INGEN_BARNEHAGEPLASS,
            -> fomDato.minusDays(1).førsteDagINesteMåned()
        }
    }
