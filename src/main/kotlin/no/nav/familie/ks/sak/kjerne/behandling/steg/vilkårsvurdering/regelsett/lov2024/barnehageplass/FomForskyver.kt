package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

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
        -> fomDato.førsteDagIInneværendeMåned()

        Graderingsforskjell.REDUKSJON,
        -> fomDato.plusMonths(1).førsteDagIInneværendeMåned()
    }
}
