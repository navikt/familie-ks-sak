package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass

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
        Graderingsforskjell.REDUKSJON_TIL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
        -> tomDato.plusDays(1).minusMonths(1).sisteDagIMåned()

        Graderingsforskjell.ØKNING,
        Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS,
        -> tomDato.sisteDagIMåned()

        Graderingsforskjell.REDUKSJON,
        -> tomDato.plusDays(1).sisteDagIMåned()
    }
}

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024() = forskyvTomBasertPåGraderingsforskjell2024(this, Graderingsforskjell.REDUKSJON)?.toYearMonth()
