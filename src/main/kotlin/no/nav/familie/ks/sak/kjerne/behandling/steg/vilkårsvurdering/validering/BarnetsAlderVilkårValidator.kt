package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import org.springframework.stereotype.Component

@Component
class BarnetsAlderVilkårValidator(
    val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021,
    val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024,
    val barnetsAlderVilkårValidator2021og2024: BarnetsAlderVilkårValidator2021og2024,
) {
    fun validerVilkårBarnetsAlder(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        behandlingSkalFølgeNyeLovendringer2024: Boolean,
    ): List<String> {
        val vilkårLovverkInformasjonForBarn = VilkårLovverkInformasjonForBarn(barn.fødselsdato)
        if (!behandlingSkalFølgeNyeLovendringer2024) {
            return barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(perioder, barn, vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021, vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021, behandlingSkalFølgeNyeLovendringer2024)
        }

        return when (vilkårLovverkInformasjonForBarn.lovverk) {
            VilkårLovverk.LOVVERK_2021_OG_2024 ->
                barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(
                    perioder,
                    barn,
                    vilkårLovverkInformasjonForBarn,
                    behandlingSkalFølgeNyeLovendringer2024,
                )

            VilkårLovverk.LOVVVERK_2021 ->
                barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                    perioder,
                    barn,
                    vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021,
                    vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021,
                    behandlingSkalFølgeNyeLovendringer2024,
                )

            VilkårLovverk.LOVVERK_2024 ->
                barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                    perioder,
                    barn,
                    vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024,
                    vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024,
                )
        }
    }
}
