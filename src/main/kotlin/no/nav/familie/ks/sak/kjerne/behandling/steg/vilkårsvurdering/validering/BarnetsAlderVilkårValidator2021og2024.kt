@file:Suppress("ktlint:standard:filename")

package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import org.springframework.stereotype.Component

@Component
class BarnetsAlderVilkårValidator2021og2024(
    val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021,
    val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024,
) {
    fun validerBarnetsAlderVilkår(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        vilkårRegelverkInformasjonForBarn: VilkårRegelverkInformasjonForBarn,
    ): List<String> {
        val barnErAdoptert = perioder.any { it.verdi.erAdopsjonOppfylt() }
        if (!barnErAdoptert && perioder.size != 2) {
            return listOf("Barnets alder vilkåret må splittes i to perioder fordi barnet fyller 1 år før og 19 måneder etter 01.08.24. Periodene må være som følgende: [${vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2021} - ${minOf(vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021, DATO_LOVENDRING_2024.minusMonths(1).sisteDagIMåned())}, ${maxOf(vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024, DATO_LOVENDRING_2024)} - ${vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2024}]")
        }
        val sortertePerioder = perioder.sortedBy { it.fom }
        val periodeLov2021 = sortertePerioder.first()
        val periodeLov2024 = sortertePerioder.last()
        val funksjonelleFeilValideringLov2021 = barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(listOf(periodeLov2021), barn, vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2021, vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2021)
        val funksjonelleFeilValideringLov2024 = barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(listOf(periodeLov2024), barn, vilkårRegelverkInformasjonForBarn.periodeFomBarnetsAlderLov2024, vilkårRegelverkInformasjonForBarn.periodeTomBarnetsAlderLov2024)

        return funksjonelleFeilValideringLov2021.plus(funksjonelleFeilValideringLov2024)
    }
}
