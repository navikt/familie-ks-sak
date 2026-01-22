package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.IkkeNullbarPeriode
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
class BarnetsAlderVilkårValidator2021 {
    fun validerBarnetsAlderVilkår(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        periodeFomBarnetsAlderLov2021: LocalDate,
        periodeTomBarnetsAlderLov2021: LocalDate,
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
                        ) -> {
                            "Du kan ikke sette en t.o.m dato på barnets alder vilkåret som er etter august året barnet fyller 6 år."
                        }

                        // Ved adopsjon skal det være lov å ha en differanse på 1 år slik at man får 11 måned med kontantstøtte.
                        it.fom.plusYears(1) < it.tom -> {
                            "Differansen mellom f.o.m datoen og t.o.m datoen på barnets alder vilkåret kan ikke være mer enn 1 år."
                        }

                        else -> {
                            null
                        }
                    }
                }

        val funksjonelleFeilForPerioderUtenAdopsjon =
            perioder
                .filter { !it.verdi.erAdopsjonOppfylt() }
                .mapNotNull {
                    val periodeTomEllerDatoFørLovendring =
                        minOf(
                            periodeTomBarnetsAlderLov2021,
                            DATO_LOVENDRING_2024.minusMonths(1).sisteDagIMåned(),
                        )
                    when {
                        !it.fom.isEqual(periodeFomBarnetsAlderLov2021) -> {
                            "F.o.m datoen på barnets alder vilkåret må være lik barnets 1 års dag."
                        }

                        !it.tom.isEqual(periodeTomEllerDatoFørLovendring) && it.tom != barn.dødsfall?.dødsfallDato -> {
                            "T.o.m datoen på barnets alder vilkåret må være lik barnets 2 års dag eller 31.07.24 på grunn av lovendring fra og med 01.08.24. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall."
                        }

                        else -> {
                            null
                        }
                    }
                }

        return funksjonelleFeilForPerioderMedAdopsjon + funksjonelleFeilForPerioderUtenAdopsjon
    }
}
