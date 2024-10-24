package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import java.time.LocalDate

fun forskyvTomBasertPåGraderingsforskjell2024(
    tomDato: LocalDate?,
    graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell,
): LocalDate? {
    if (tomDato == null) {
        return null
    }
    return when (graderingsforskjellMellomDenneOgNestePeriode) {
        Graderingsforskjell.LIK,
        Graderingsforskjell.REDUKSJON,
        Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
        -> tomDato.plusDays(1).minusMonths(1).sisteDagIMåned()

        Graderingsforskjell.ØKNING,
        -> tomDato.sisteDagIMåned()

        Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS,
        -> tomDato.plusDays(1).sisteDagIMåned()
    }
}

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024() =
    forskyvTomBasertPåGraderingsforskjell2024(this, Graderingsforskjell.REDUKSJON)?.toYearMonth()
