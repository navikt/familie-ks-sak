package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.barnehageplass

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import java.math.BigDecimal

enum class Graderingsforskjell {
    INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
    ØKNING,
    ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
    REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
    REDUKSJON,
    LIK,
    INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
}

fun finnGraderingsforskjellMellomDenneOgForrigePeriode(
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

    val erFørstePeriode = vilkårResultatForrigePeriode == null

    val gikkPåBarnehageForrigePeriode = vilkårResultatForrigePeriode?.vilkårResultat?.antallTimer != null
    val gårIkkePåBarnehageDennePerioden = vilkårResultat?.antallTimer == null

    val sluttetIBarnehageDennePerioden = gikkPåBarnehageForrigePeriode && gårIkkePåBarnehageDennePerioden

    return when {
        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden == BigDecimal.ZERO -> Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.REDUKSJON
        sluttetIBarnehageDennePerioden -> Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE
        graderingForrigePeriode < graderingDennePerioden && graderingForrigePeriode == BigDecimal.ZERO -> if (erFørstePeriode) Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING else Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING
        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.ØKNING
        else -> Graderingsforskjell.LIK
    }
}
