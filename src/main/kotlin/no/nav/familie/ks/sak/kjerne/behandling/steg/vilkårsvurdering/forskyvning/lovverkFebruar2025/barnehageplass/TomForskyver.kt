package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import java.time.LocalDate

fun forskyvTomBasertPåGraderingsforskjell(
    tomDato: LocalDate?,
    graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell,
): LocalDate? {
    if (tomDato == null) {
        return null
    }
    return when (graderingsforskjellMellomDenneOgNestePeriode) {
        Graderingsforskjell.LIK,
        Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
        Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
        Graderingsforskjell.ØKNING,
        -> tomDato.sisteDagIMåned()

        Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
        -> tomDato.plusDays(1).sisteDagIMåned()

        Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
        Graderingsforskjell.REDUKSJON,
        -> tomDato.plusDays(1).minusMonths(1).sisteDagIMåned()
    }
}

fun LocalDate?.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2025() = forskyvTomBasertPåGraderingsforskjell(this, Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING)?.toYearMonth()
