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
        -> tomDato.minusMonths(1).sisteDagIMåned()

        Graderingsforskjell.ØKNING,
        -> tomDato.sisteDagIMåned()

        Graderingsforskjell.REDUKSJON,
        -> tomDato.plusDays(1).sisteDagIMåned()
    }
}

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2024() =
    forskyvTomBasertPåGraderingsforskjell2024(this, Graderingsforskjell.REDUKSJON)?.toYearMonth()
