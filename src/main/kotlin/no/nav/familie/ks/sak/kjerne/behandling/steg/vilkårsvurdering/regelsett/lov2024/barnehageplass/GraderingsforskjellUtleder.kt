package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal
import java.time.YearMonth

fun hentGraderingsforskjellMellomDenneOgForrigePeriode2024(
    vilkårResultat: VilkårResultat?,
    vilkårResultatForrigePeriode: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?,
    tidligsteÅrMånedAlleAndreVilkårErOppfylt: YearMonth,
): Graderingsforskjell {
    val graderingForrigePeriode =
        vilkårResultatForrigePeriode?.vilkårResultat?.let {
            hentProsentForAntallTimer(vilkårResultatForrigePeriode.vilkårResultat.antallTimer)
        } ?: BigDecimal.ZERO

    val graderingDennePerioden =
        vilkårResultat?.let {
            hentProsentForAntallTimer(vilkårResultat.antallTimer)
        } ?: BigDecimal.ZERO

    val fomErSammeMånedSomAlleAndreVilkårBlirOppfylt = vilkårResultat?.periodeFom?.toYearMonth() == tidligsteÅrMånedAlleAndreVilkårErOppfylt

    return when {
        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden.equals(BigDecimal(0)) && fomErSammeMånedSomAlleAndreVilkårBlirOppfylt -> Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT

        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden.equals(BigDecimal(0)) -> Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS

        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.REDUKSJON
        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.ØKNING
        else -> Graderingsforskjell.LIK
    }
}
