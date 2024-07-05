package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import java.time.temporal.ChronoUnit

fun utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn: VilkårLovverkInformasjonForBarn): Long =
    when (vilkårLovverkInformasjonForBarn.lovverk) {
        VilkårLovverk.LOVVERK_2024,
        VilkårLovverk.LOVVERK_2021_OG_2024,
        -> 7L
        VilkårLovverk.LOVVERK_2021 -> {
            val sisteMuligeUtbetaling =
                minOf(
                    vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021.tilYearMonth(),
                    DATO_LOVENDRING_2024.toYearMonth(),
                ).minusMonths(1)
            val førsteMuligeUtbetaling = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021.tilYearMonth().plusMonths(1)

            førsteMuligeUtbetaling.until(sisteMuligeUtbetaling, ChronoUnit.MONTHS) + 1L
        }
    }
