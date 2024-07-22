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
    ): List<String> {
        val vilkårLovverkInformasjonForBarn = VilkårLovverkInformasjonForBarn(barn.fødselsdato)

        return when (vilkårLovverkInformasjonForBarn.lovverk) {
            VilkårLovverk.LOVVERK_2021_OG_2024 ->
                barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    vilkårLovverkInformasjonForBarn = vilkårLovverkInformasjonForBarn,
                )

            VilkårLovverk.LOVVERK_2021 ->
                barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    periodeFomBarnetsAlderLov2021 = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021,
                    periodeTomBarnetsAlderLov2021 = vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021,
                )

            VilkårLovverk.LOVVERK_2024 ->
                barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    periodeFomBarnetsAlderLov2024 = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024,
                    periodeTomBarnetsAlderLov2024 = vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024,
                )
        }
    }
}
