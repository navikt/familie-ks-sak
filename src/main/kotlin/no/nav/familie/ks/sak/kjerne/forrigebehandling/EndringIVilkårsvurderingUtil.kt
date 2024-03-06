package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvVilkårResultater

object EndringIVilkårsvurderingUtil {
    fun lagEndringIVilkårsvurderingTidslinje(
        nåværendePersonResultaterForPerson: Set<PersonResultat>,
        forrigePersonResultater: Set<PersonResultat>,
    ): Tidslinje<Boolean> {
        val tidslinjePerVilkår =
            Vilkår.entries.map { vilkår ->
                val vilkårTidslinje =
                    lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
                        nåværendeOppfylteVilkårResultaterForPerson =
                        nåværendePersonResultaterForPerson
                            .flatMap { it.vilkårResultater }
                            .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                        forrigeOppfylteVilkårResultaterForPerson =
                        forrigePersonResultater
                            .flatMap { it.vilkårResultater }
                            .filter { it.vilkårType == vilkår && it.resultat == Resultat.OPPFYLT },
                        vilkår = vilkår,
                    )
                vilkårTidslinje
            }

        return tidslinjePerVilkår.kombiner { finnesMinstEnEndringIPeriode(it) }
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }

    // Relevante endringer er
    // 1. Endringer i utdypende vilkårsvurdering
    // 2. Endringer i regelverk
    // 3. Splitt i vilkårsvurderingen
    private fun lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
        nåværendeOppfylteVilkårResultaterForPerson: List<VilkårResultat>,
        forrigeOppfylteVilkårResultaterForPerson: List<VilkårResultat>,
        vilkår: Vilkår,
    ): Tidslinje<Boolean> {
        val nåværendeVilkårResultatTidslinje = forskyvVilkårResultater(vilkår, nåværendeOppfylteVilkårResultaterForPerson).tilTidslinje()
        val tidligereVilkårResultatTidslinje = forskyvVilkårResultater(vilkår, forrigeOppfylteVilkårResultaterForPerson).tilTidslinje()

        val endringIVilkårResultat =
            nåværendeVilkårResultatTidslinje.kombinerMed(tidligereVilkårResultatTidslinje) { nåværende, forrige ->
                if (nåværende == null || forrige == null) return@kombinerMed false

                val erEndringerIUtdypendeVilkårsvurdering =
                    nåværende.utdypendeVilkårsvurderinger.toSet() != forrige.utdypendeVilkårsvurderinger.toSet()
                val erEndringerIRegelverk = nåværende.vurderesEtter != forrige.vurderesEtter
                val erVilkårSomErSplittetOpp = nåværende.periodeFom != forrige.periodeFom

                (forrige.obligatoriskUtdypendeVilkårsvurderingErSatt() && erEndringerIUtdypendeVilkårsvurdering) ||
                        erEndringerIRegelverk ||
                        erVilkårSomErSplittetOpp
            }

        return endringIVilkårResultat
    }

    private fun VilkårResultat.obligatoriskUtdypendeVilkårsvurderingErSatt(): Boolean {
        return this.utdypendeVilkårsvurderinger.isNotEmpty() || !this.utdypendeVilkårsvurderingErObligatorisk()
    }

    private fun VilkårResultat.utdypendeVilkårsvurderingErObligatorisk(): Boolean {
        return if (this.vurderesEtter == Regelverk.NASJONALE_REGLER) {
            false
        } else {
            when (this.vilkårType) {
                Vilkår.BOSATT_I_RIKET,
                Vilkår.BOR_MED_SØKER,
                -> true

                Vilkår.BARNETS_ALDER,
                Vilkår.LOVLIG_OPPHOLD,
                Vilkår.BARNEHAGEPLASS,
                Vilkår.MEDLEMSKAP,
                Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                Vilkår.LOVLIG_OPPHOLD,
                -> false
            }
        }
    }
}
