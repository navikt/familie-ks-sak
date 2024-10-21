package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import java.time.LocalDate

fun forskyvTomBasertPåGraderingsforskjell2024(
    localDate: LocalDate?,
    graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell,
) =
    localDate?.let { tomDato ->
        when (graderingsforskjellMellomDenneOgNestePeriode) {
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
