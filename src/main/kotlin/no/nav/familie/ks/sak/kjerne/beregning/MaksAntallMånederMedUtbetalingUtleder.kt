package no.nav.familie.ks.sak.kjerne.beregning

import java.time.temporal.ChronoUnit
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn

fun utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn: VilkårLovverkInformasjonForBarn): Long {
    return when(vilkårLovverkInformasjonForBarn.lovverk) {
        VilkårLovverk._2021_OG_2024 -> 7L
        VilkårLovverk._2021 -> {
            val tomBarnetsAlderEllerDatoForLovendring =
                minOf(
                    vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021.tilYearMonth(),
                    DATO_LOVENDRING_2024.tilYearMonth(),
                )
            val fomBarnetsAlder = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024.tilYearMonth()
            fomBarnetsAlder.until(tomBarnetsAlderEllerDatoForLovendring, ChronoUnit.MONTHS)
        }
        VilkårLovverk._2024 -> 7L
    }
}
