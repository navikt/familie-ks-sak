package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import java.time.LocalDate
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned

fun forskyvFomBasertPåGraderingsforskjell2024(localDate: LocalDate?, graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell) =
    localDate?.let { fomDato ->
        when (graderingsforskjellMellomDenneOgForrigePeriode) {
            Graderingsforskjell.LIK,
            Graderingsforskjell.ØKNING,
            Graderingsforskjell.REDUKSJON,
            Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS,
            Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
            -> fomDato.førsteDagIInneværendeMåned()
        }
    }
