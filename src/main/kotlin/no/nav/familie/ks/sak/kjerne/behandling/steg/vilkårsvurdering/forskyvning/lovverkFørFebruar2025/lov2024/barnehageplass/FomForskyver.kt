package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import java.time.LocalDate

fun forskyvFomBasertPåGraderingsforskjell2024(
    fomDato: LocalDate?,
    graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell,
): LocalDate? {
    if (fomDato == null) {
        return null
    }
    return when (graderingsforskjellMellomDenneOgForrigePeriode) {
        Graderingsforskjell.LIK,
        Graderingsforskjell.ØKNING,
        Graderingsforskjell.REDUKSJON_TIL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
        -> fomDato.førsteDagIInneværendeMåned()

        Graderingsforskjell.REDUKSJON,
        -> fomDato.plusMonths(1).førsteDagIInneværendeMåned()

        Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS,
        -> fomDato.minusDays(1).plusMonths(1).førsteDagIInneværendeMåned()
    }
}
