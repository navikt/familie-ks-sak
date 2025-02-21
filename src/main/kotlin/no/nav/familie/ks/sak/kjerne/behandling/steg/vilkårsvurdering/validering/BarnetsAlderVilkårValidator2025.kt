package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.IkkeNullbarPeriode
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
class BarnetsAlderVilkårValidator2025 {
    fun validerBarnetsAlderVilkår(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        periodeFomBarnetsAlderLov2025: LocalDate,
        periodeTomBarnetsAlderLov2025: LocalDate,
    ): List<String> {
        if (perioder.isEmpty()) {
            return emptyList()
        }

        val funksjonelleFeilForPerioderMedAdopsjon =
            perioder
                .filter { it.verdi.erAdopsjonOppfylt() }
                .mapNotNull {
                    when {
                        it.tom.isAfter(
                            barn.fødselsdato
                                .plusYears(6)
                                .withMonth(Month.AUGUST.value)
                                .sisteDagIMåned(),
                        ) ->
                            "Du kan ikke sette en t.o.m dato på barnets aldersvilkår som er etter august året barnet fyller 6 år."
                        // Ved adopsjon skal det være lov å ha en differanse på 8 måneder slik at man får 7 måned med kontantstøtte.
                        it.fom.plusMonths(8) < it.tom ->
                            "Differansen mellom f.o.m datoen og t.o.m datoen på barnets aldersvilkår kan ikke være mer enn 8 måneder."

                        else -> null
                    }
                }

        val funksjonelleFeilForPerioderUtenAdopsjon =
            perioder
                .filter { !it.verdi.erAdopsjonOppfylt() }
                .mapNotNull {
                    when {
                        !it.fom.isEqual(periodeFomBarnetsAlderLov2025) ->
                            "F.o.m datoen på barnets aldersvilkår må være lik datoen barnet fyller 12 måneder."

                        !it.tom.isEqual(periodeTomBarnetsAlderLov2025) && it.tom != barn.dødsfall?.dødsfallDato ->
                            "T.o.m datoen på barnets aldersvilkår må være lik datoen barnet fyller 20 måneder. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall."

                        else -> null
                    }
                }

        return funksjonelleFeilForPerioderMedAdopsjon + funksjonelleFeilForPerioderUtenAdopsjon
    }
}
