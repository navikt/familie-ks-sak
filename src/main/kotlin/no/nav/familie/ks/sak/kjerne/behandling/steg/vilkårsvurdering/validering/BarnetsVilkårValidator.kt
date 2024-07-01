package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.IkkeNullbarPeriode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import org.springframework.stereotype.Component

@Component
class BarnetsVilkårValidator(
    val barnetsAlderVilkårValidator: BarnetsAlderVilkårValidator,
) {
    fun validerAtDatoErKorrektIBarnasVilkår(
        vilkårsvurdering: Vilkårsvurdering,
        barna: List<Person>,
        behandlingSkalFølgeNyeLovendringer2024: Boolean,
    ) {
        val funksjonelleFeil = mutableListOf<String>()

        barna.map { barn ->
            val vilkårsResultaterForBarn =
                vilkårsvurdering.personResultater
                    .flatMap { it.vilkårResultater }
                    .filter { it.personResultat?.aktør == barn.aktør }

            vilkårsResultaterForBarn.forEach { vilkårResultat ->
                val fødselsdato = barn.fødselsdato.tilDagMånedÅr()
                val vilkårType = vilkårResultat.vilkårType
                if (vilkårResultat.resultat == Resultat.OPPFYLT && vilkårResultat.periodeFom == null) {
                    funksjonelleFeil.add("Vilkår $vilkårType for barn med fødselsdato $fødselsdato mangler fom dato.")
                }
                if (vilkårResultat.periodeFom != null &&
                    vilkårType != Vilkår.MEDLEMSKAP_ANNEN_FORELDER &&
                    vilkårResultat.lagOgValiderPeriodeFraVilkår().fom.isBefore(barn.fødselsdato)
                ) {
                    funksjonelleFeil.add(
                        "Vilkår $vilkårType for barn med fødselsdato $fødselsdato " +
                            "har fom dato før barnets fødselsdato.",
                    )
                }
            }

            val barnetsAlderVilkårSomSkalValideres =
                vilkårsResultaterForBarn
                    .filter { it.vilkårType == Vilkår.BARNETS_ALDER }
                    .filter { it.periodeFom != null }
                    .filter { it.erEksplisittAvslagPåSøknad != true }

            val funksjonelleFeilBarnetsAlder =
                barnetsAlderVilkårValidator.validerVilkårBarnetsAlder(
                    barnetsAlderVilkårSomSkalValideres.map { it.lagOgValiderPeriodeFraVilkår() },
                    barn,
                    behandlingSkalFølgeNyeLovendringer2024,
                )
            funksjonelleFeil.addAll(funksjonelleFeilBarnetsAlder)
        }

        if (funksjonelleFeil.isNotEmpty()) {
            throw FunksjonellFeil(funksjonelleFeil.joinToString(separator = "\n"))
        }
    }

    private fun VilkårResultat.lagOgValiderPeriodeFraVilkår(): IkkeNullbarPeriode<VilkårResultat> =
        when {
            periodeFom !== null -> {
                IkkeNullbarPeriode(verdi = this, fom = checkNotNull(periodeFom), tom = periodeTom ?: TIDENES_ENDE)
            }

            erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
                IkkeNullbarPeriode(verdi = this, fom = TIDENES_MORGEN, tom = TIDENES_ENDE)
            }

            else -> {
                throw FunksjonellFeil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
            }
        }
}
