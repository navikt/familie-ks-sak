package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.IkkeNullbarPeriode
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BarnetsAlderVilkårValidator(
    val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021,
    val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024,
    val barnetsAlderVilkårValidator2021og2024: BarnetsAlderVilkårValidator2021og2024,
    val barnetsAlderVilkårValidator2025: BarnetsAlderVilkårValidator2025,
) {
    fun validerVilkårBarnetsAlder(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        adopsjonsdato: LocalDate?,
    ): List<String> {
        val vilkårLovverkInformasjonForBarn =
            if (perioder.any { it.verdi.erAdopsjonOppfylt() }) {
                VilkårLovverkInformasjonForBarn(
                    fødselsdato = barn.fødselsdato,
                    adopsjonsdato = adopsjonsdato,
                    periodeFomForAdoptertBarn = perioder.minOf { it.fom }.toYearMonth(),
                    periodeTomForAdoptertBarn = perioder.maxOf { it.tom }.toYearMonth(),
                )
            } else {
                VilkårLovverkInformasjonForBarn(fødselsdato = barn.fødselsdato, adopsjonsdato = adopsjonsdato)
            }

        return when (vilkårLovverkInformasjonForBarn.vilkårLovverk) {
            VilkårLovverk.LOVVERK_2021_OG_2024 -> {
                barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    vilkårLovverkInformasjonForBarn = vilkårLovverkInformasjonForBarn,
                )
            }

            VilkårLovverk.LOVVERK_2021 -> {
                barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    periodeFomBarnetsAlderLov2021 = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2021,
                    periodeTomBarnetsAlderLov2021 = vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2021,
                )
            }

            VilkårLovverk.LOVVERK_2024 -> {
                barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    periodeFomBarnetsAlderLov2024 = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2024,
                    periodeTomBarnetsAlderLov2024 = vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2024,
                )
            }

            VilkårLovverk.LOVVERK_2025 -> {
                barnetsAlderVilkårValidator2025.validerBarnetsAlderVilkår(
                    perioder = perioder,
                    barn = barn,
                    periodeFomBarnetsAlderLov2025 = vilkårLovverkInformasjonForBarn.periodeFomBarnetsAlderLov2025,
                    periodeTomBarnetsAlderLov2025 = vilkårLovverkInformasjonForBarn.periodeTomBarnetsAlderLov2025,
                )
            }
        }
    }
}
