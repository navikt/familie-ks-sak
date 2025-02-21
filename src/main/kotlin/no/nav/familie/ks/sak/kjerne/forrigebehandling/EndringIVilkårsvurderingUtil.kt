package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

object EndringIVilkårsvurderingUtil {
    fun utledEndringstidspunktForVilkårsvurdering(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
    ): YearMonth? {
        val nåværendeAktørerMedVilkårsvurdering = nåværendePersonResultat.map { it.aktør }
        val forrigeAktørerMedVilkårsvurdering = forrigePersonResultat.map { it.aktør }

        val allePersonerMedVilkårsvurdering = (nåværendeAktørerMedVilkårsvurdering + forrigeAktørerMedVilkårsvurdering).distinct()

        val endringIVilkårsvurderingTidslinjer =
            allePersonerMedVilkårsvurdering
                .map { aktør ->
                    lagEndringIVilkårsvurderingTidslinje(
                        nåværendePersonResultat = nåværendePersonResultat.filter { it.aktør == aktør }.singleOrNull(),
                        forrigePersonResultat = forrigePersonResultat.filter { it.aktør == aktør }.singleOrNull(),
                    )
                }.kombiner { finnesMinstEnEndringIPeriode(it) }

        return endringIVilkårsvurderingTidslinjer.tilFørsteEndringstidspunkt()
    }

    fun lagEndringIVilkårsvurderingTidslinje(
        nåværendePersonResultat: PersonResultat?,
        forrigePersonResultat: PersonResultat?,
    ): Tidslinje<Boolean> {
        val tidslinjePerVilkår =
            Vilkår.entries.filter { it != Vilkår.BARNETS_ALDER }.map { vilkår ->
                lagEndringIVilkårsvurderingForPersonOgVilkårTidslinje(
                    nåværendeVilkårTidslinje = nåværendePersonResultat?.vilkårResultater?.filter { it.erOppfylt() && it.vilkårType == vilkår }?.tilTidslinje() ?: tomTidslinje(),
                    tidligereVilkårTidslinje = forrigePersonResultat?.vilkårResultater?.filter { it.erOppfylt() && it.vilkårType == vilkår }?.tilTidslinje() ?: tomTidslinje(),
                )
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
        nåværendeVilkårTidslinje: Tidslinje<VilkårResultat>,
        tidligereVilkårTidslinje: Tidslinje<VilkårResultat>,
    ): Tidslinje<Boolean> {
        val endringIVilkårResultat =
            nåværendeVilkårTidslinje.kombinerMed(tidligereVilkårTidslinje) { nåværende, forrige ->
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
