package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal

enum class Graderingsforskjell {
    ØKNING,
    REDUKSJON,
    LIK,
}

fun finnGraderingsforskjellMellomDenneOgForrigePeriode2024(
    vilkårResultat: VilkårResultat?,
    vilkårResultatForrigePeriode: BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>?,
): Graderingsforskjell {
    val graderingForrigePeriode =
        vilkårResultatForrigePeriode?.vilkårResultat?.let {
            hentProsentForAntallTimer(vilkårResultatForrigePeriode.vilkårResultat.antallTimer)
        } ?: BigDecimal.ZERO

    val graderingDennePerioden =
        vilkårResultat?.let {
            hentProsentForAntallTimer(vilkårResultat.antallTimer)
        } ?: BigDecimal.ZERO

    return when {
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.REDUKSJON
        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.ØKNING
        else -> Graderingsforskjell.LIK
    }
}
