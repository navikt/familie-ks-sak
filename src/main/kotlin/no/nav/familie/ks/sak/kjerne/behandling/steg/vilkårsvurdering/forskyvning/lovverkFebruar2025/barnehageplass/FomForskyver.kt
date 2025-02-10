package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import java.time.LocalDate

fun forskyvFomBasertPåGraderingsforskjell(
    fomDato: LocalDate?,
    graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell,
): LocalDate? {
    if (fomDato == null) {
        return null
    }
    return when (graderingsforskjellMellomDenneOgForrigePeriode) {
        Graderingsforskjell.LIK,
        Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
        Graderingsforskjell.ØKNING,
        -> fomDato.minusDays(1).plusMonths(1).førsteDagIInneværendeMåned()

        Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
        -> fomDato.minusDays(1).plusMonths(2).førsteDagIInneværendeMåned()

        Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
        -> fomDato.plusMonths(1).førsteDagIInneværendeMåned()

        Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
        Graderingsforskjell.REDUKSJON,
        -> fomDato.førsteDagIInneværendeMåned()
    }
}
