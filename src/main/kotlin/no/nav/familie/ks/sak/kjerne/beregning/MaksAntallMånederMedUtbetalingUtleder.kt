package no.nav.familie.ks.sak.kjerne.beregning

import java.time.temporal.ChronoUnit
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn

fun utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn: VilkårRegelverkInformasjonForBarn): Long {
    return when {
        vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 && vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 -> 7L
        vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 -> {
            val tomBarnetsAlderEllerDatoForLovendring =
                minOf(
                    vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021.tilYearMonth(),
                    DATO_LOVENDRING_2024.tilYearMonth(),
                )
            val fomBarnetsAlder = vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024.tilYearMonth()
            fomBarnetsAlder.until(tomBarnetsAlderEllerDatoForLovendring, ChronoUnit.MONTHS)
        }
        vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 -> 7L
        else -> throw Feil(
            "Barnets vilkår blir verken truffet av 2021 eller 2024 regelverket. Dette skal ikke være mulig.",
        )
    }
}
