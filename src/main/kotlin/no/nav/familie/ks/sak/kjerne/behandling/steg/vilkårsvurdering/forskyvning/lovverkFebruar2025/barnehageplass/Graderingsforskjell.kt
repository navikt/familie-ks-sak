package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass

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
    vilkårResultatForrigePeriode: VilkårResultat?,
    vilkårResultatDennePerioden: VilkårResultat?,
    erFørstePeriode: Boolean,
): Graderingsforskjell {
    val graderingForrigePeriode =
        if (vilkårResultatForrigePeriode != null) {
            hentProsentForAntallTimer(vilkårResultatForrigePeriode.antallTimer)
        } else {
            BigDecimal.ZERO
        }

    val graderingDennePerioden =
        if (vilkårResultatDennePerioden != null) {
            hentProsentForAntallTimer(vilkårResultatDennePerioden.antallTimer)
        } else {
            BigDecimal.ZERO
        }

    val gikkIBarnehageForrigePeriode = vilkårResultatForrigePeriode?.antallTimer != null
    val gårIkkeIBarnehageDennePerioden = vilkårResultatDennePerioden?.antallTimer == null

    val sluttetIBarnehageDennePerioden = gikkIBarnehageForrigePeriode && gårIkkeIBarnehageDennePerioden

    return when {
        graderingForrigePeriode > graderingDennePerioden && graderingDennePerioden == BigDecimal.ZERO -> Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING
        graderingForrigePeriode > graderingDennePerioden -> Graderingsforskjell.REDUKSJON
        graderingForrigePeriode < graderingDennePerioden && graderingForrigePeriode == BigDecimal.ZERO -> if (erFørstePeriode) Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING else Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING
        sluttetIBarnehageDennePerioden -> Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE
        graderingForrigePeriode < graderingDennePerioden -> Graderingsforskjell.ØKNING
        else -> Graderingsforskjell.LIK
    }
}
