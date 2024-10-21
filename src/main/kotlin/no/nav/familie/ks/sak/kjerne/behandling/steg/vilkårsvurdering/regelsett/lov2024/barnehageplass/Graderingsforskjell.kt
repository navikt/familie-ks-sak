package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal
import java.time.YearMonth

enum class Graderingsforskjell {
    ØKNING,
    REDUKSJON,
    REDUKSJON_TIL_FULL_BARNEHAGEPLASS,
    REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
    LIK,
}

fun utledGraderingsforskjellMellomDenneOgForrigePeriode2024(
    vilkårResultatForrigePerioden: VilkårResultat?,
    vilkårResultatDennePerioden: VilkårResultat?,
    tidligsteÅrMånedAlleAndreVilkårErOppfylt: YearMonth?,
): Graderingsforskjell {
    val graderingForrigePeriode =
        vilkårResultatForrigePerioden?.let {
            hentProsentForAntallTimer(it.antallTimer)
        } ?: BigDecimal.ZERO

    val graderingDennePerioden =
        vilkårResultatDennePerioden?.let {
            hentProsentForAntallTimer(it.antallTimer)
        } ?: BigDecimal.ZERO

    val fomErSammeMånedSomAlleAndreVilkårBlirOppfylt =
        tidligsteÅrMånedAlleAndreVilkårErOppfylt != null &&
            vilkårResultatDennePerioden?.periodeFom?.toYearMonth() == tidligsteÅrMånedAlleAndreVilkårErOppfylt

    return when {
        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden.equals(BigDecimal(0)) && fomErSammeMånedSomAlleAndreVilkårBlirOppfylt
        -> Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT

        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden.equals(BigDecimal(0))
        -> Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS

        graderingForrigePeriode > graderingDennePerioden
        -> Graderingsforskjell.REDUKSJON

        graderingForrigePeriode < graderingDennePerioden
        -> Graderingsforskjell.ØKNING

        else -> Graderingsforskjell.LIK
    }
}
