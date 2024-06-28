package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import java.time.temporal.ChronoUnit

fun utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn: VilkårLovverkInformasjonForBarn): Long {
    return when (vilkårLovverkInformasjonForBarn.lovverk) {
        VilkårLovverk.LOVVERK_2021_OG_2024 -> 7L
        VilkårLovverk.LOVVVERK_2021 -> {
            val tomBarnetsAlderEllerDatoForLovendring =
                minOf(
                    vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021.tilYearMonth(),
                    DATO_LOVENDRING_2024.tilYearMonth(),
                )
            val fomBarnetsAlder = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024.tilYearMonth()
            fomBarnetsAlder.until(tomBarnetsAlderEllerDatoForLovendring, ChronoUnit.MONTHS)
        }

        VilkårLovverk.LOVVERK_2024 -> 7L
    }
}
