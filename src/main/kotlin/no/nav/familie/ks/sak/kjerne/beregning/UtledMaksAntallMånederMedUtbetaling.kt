package no.nav.familie.ks.sak.kjerne.beregning

import java.time.Period
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn

fun utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn: VilkårRegelverkInformasjonForBarn): Long {
    return when {
        vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 && vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 -> 7L
        vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 -> {
            val diff =
                Period.between(
                    vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024,
                    minOf(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021, DATO_LOVENDRING_2024.minusDays(1)),
                )
            diff.toTotalMonths()
        }
        vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 -> 7L
        else -> throw FunksjonellFeil("asdf")
    }
}
