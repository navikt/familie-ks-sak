package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.forskyvVilkårResultater
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed

object EndringIVilkårsvurderingUtil {
    fun lagEndringIVilkårsvurderingTidslinje(
        personResultat: PersonResultat?,
        forrigePersonResultat: PersonResultat?,
    ): Tidslinje<Boolean> {
        val nåværendeVilkårResultatTidslinjePerVilkår = personResultat?.forskyvVilkårResultater()?.mapValues { it.value.filter { periode -> periode.verdi.erOppfylt() }.tilTidslinje() } ?: emptyMap()
        val tidligereVilkårResultatTidslinjePerVilkår = forrigePersonResultat?.forskyvVilkårResultater()?.mapValues { it.value.filter { periode -> periode.verdi.erOppfylt() }.tilTidslinje() } ?: emptyMap()

        val tidslinjePerVilkår =
            nåværendeVilkårResultatTidslinjePerVilkår
                .entries
                .filter { it.key != Vilkår.BARNETS_ALDER }
                .map { lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(it.value, tidligereVilkårResultatTidslinjePerVilkår[it.key] ?: tomTidslinje()) }

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
        nåværendeVilkårResultatTidslinje: Tidslinje<VilkårResultat>,
        tidligereVilkårResultatTidslinje: Tidslinje<VilkårResultat>,
    ): Tidslinje<Boolean> {
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

    private fun VilkårResultat.obligatoriskUtdypendeVilkårsvurderingErSatt(): Boolean = this.utdypendeVilkårsvurderinger.isNotEmpty() || !this.utdypendeVilkårsvurderingErObligatorisk()

    private fun VilkårResultat.utdypendeVilkårsvurderingErObligatorisk(): Boolean =
        if (this.vurderesEtter == Regelverk.NASJONALE_REGLER) {
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
                -> false
            }
        }
}
