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
class BarnetsAlderVilkårValidator2024 {
    fun validerBarnetsAlderVilkår(
        perioder: List<IkkeNullbarPeriode<VilkårResultat>>,
        barn: Person,
        periodeFomBarnetsAlderLov2024: LocalDate,
        periodeTomBarnetsAlderLov2024: LocalDate,
    ): List<String> {
        if (perioder.isEmpty()) return emptyList()
        val funksjonelleFeil =
            perioder.map {
                when {
                    it.verdi.erAdopsjonOppfylt() &&
                        it.tom.isAfter(
                            barn.fødselsdato
                                .plusYears(6)
                                .withMonth(Month.AUGUST.value)
                                .sisteDagIMåned(),
                        ) -> {
                        "Du kan ikke sette en t.o.m dato på barnets alder vilkåret som er etter august året barnet fyller 6 år."
                    }

                    it.verdi.erAdopsjonOppfylt() && it.fom.plusMonths(6) < it.tom -> {
                        "Differansen mellom f.o.m datoen og t.o.m datoen på barnets alder vilkåret kan ikke være mer enn 6 måneder."
                    }

                    !it.verdi.erAdopsjonOppfylt() && !it.fom.isEqual(maxOf(periodeFomBarnetsAlderLov2024, DATO_LOVENDRING_2024)) -> {
                        "F.o.m datoen på barnets alder vilkåret må være lik datoen barnet fyller 13 måneder eller 01.08.24 dersom barnet fyller 13 måneder før 01.08.24."
                    }

                    !it.verdi.erAdopsjonOppfylt() && !it.tom.isEqual(periodeTomBarnetsAlderLov2024) && it.tom != barn.dødsfall?.dødsfallDato -> {
                        "T.o.m datoen på barnets alder vilkåret må være lik datoen barnet fyller 19 måneder. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall."
                    }

                    else -> {
                        null
                    }
                }
            }
        return funksjonelleFeil.filterNotNull()
    }
}
