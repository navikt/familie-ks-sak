package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.IkkeNullbarPeriode
import org.springframework.stereotype.Component

@Component
class BarnetsAlderVilkårValidator2021og2024(
    val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021,
    val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024,
) {
    fun validerBarnetsAlderVilkår(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        vilkårLovverkInformasjonForBarn: VilkårLovverkInformasjonForBarn,
    ): List<String> {
        val påkrevdAntallBarnetsAlderPerioderPåBarn = 2

        if (perioder.size != påkrevdAntallBarnetsAlderPerioderPåBarn) {
            return listOf("Vilkåret for barnets alder må splittes i to perioder fordi den strekker seg over lovendringen 01.08.2024. Henlegg denne behandlingen og opprett en ny behandling. I den nye behandlingen vil splitten dannes automatisk.")
        }

        val sortertePerioder = perioder.sortedBy { it.fom }
        val periodeLov2021 = sortertePerioder.first()
        val periodeLov2024 = sortertePerioder.last()
        val funksjonelleFeilValideringLov2021 = barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(listOf(periodeLov2021), barn, vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021, vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021)
        val funksjonelleFeilValideringLov2024 = barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(listOf(periodeLov2024), barn, vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024, vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024)
        return funksjonelleFeilValideringLov2021.plus(funksjonelleFeilValideringLov2024)
    }
}
