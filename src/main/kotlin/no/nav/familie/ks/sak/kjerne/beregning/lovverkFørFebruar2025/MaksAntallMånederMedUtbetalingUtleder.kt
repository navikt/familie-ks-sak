package no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import java.time.temporal.ChronoUnit

fun utledMaksAntallMånederMedUtbetaling(
    vilkårLovverkInformasjonForBarn: VilkårLovverkInformasjonForBarn,
    barnetsAlderVilkårResultater: List<VilkårResultat>,
): Long =
    when (vilkårLovverkInformasjonForBarn.vilkårLovverk) {
        VilkårLovverk.LOVVERK_2025,
        VilkårLovverk.LOVVERK_2024,
        VilkårLovverk.LOVVERK_2021_OG_2024,
        -> {
            7L
        }

        VilkårLovverk.LOVVERK_2021 -> {
            val førsteBarnetsAlderVilkårResultatFom = barnetsAlderVilkårResultater.sortedBy { it.periodeFom }.first().periodeFom!!
            val sisteBarnetsAlderVilkårResultatTom = barnetsAlderVilkårResultater.sortedBy { it.periodeTom }.last().periodeTom!!
            val minstAvTomEllerLovEndringDato = minOf(sisteBarnetsAlderVilkårResultatTom, DATO_LOVENDRING_2024)
            val dagenFørLovendring = DATO_LOVENDRING_2024.minusDays(1)

            val toÅrsdagTilBarnetErIkkeLikDagenFørLovendring = vilkårLovverkInformasjonForBarn.fødselsdato.plusYears(2) != dagenFørLovendring
            val sisteMuligeUtbetaling =
                when {
                    toÅrsdagTilBarnetErIkkeLikDagenFørLovendring &&
                        sisteBarnetsAlderVilkårResultatTom
                        == dagenFørLovendring -> minstAvTomEllerLovEndringDato

                    else -> minstAvTomEllerLovEndringDato.minusMonths(1)
                }

            val førsteMuligeUtbetaling = førsteBarnetsAlderVilkårResultatFom.tilYearMonth().plusMonths(1)

            førsteMuligeUtbetaling.until(sisteMuligeUtbetaling, ChronoUnit.MONTHS) + 1L
        }
    }
