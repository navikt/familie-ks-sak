package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn
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
        val vilkårRegelverkInformasjonForBarn = VilkårRegelverkInformasjonForBarn(barn.fødselsdato)
        return when {
            vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 && vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 -> barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(perioder, barn, vilkårRegelverkInformasjonForBarn)
            vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 -> barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(perioder, barn, vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2021, vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021)
            vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 -> barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(perioder, barn, vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024, vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2024)
            else -> emptyList()
        }
    }
}
