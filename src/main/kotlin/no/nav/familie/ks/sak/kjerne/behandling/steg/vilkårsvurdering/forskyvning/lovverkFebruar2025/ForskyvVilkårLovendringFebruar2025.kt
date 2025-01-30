package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass.forskyvBarnehageplassVilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.standard.forskyvStandardVilkår
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

object ForskyvVilkårLovendringFebruar2025 {
    fun forskyvVilkårResultater(
        vilkårResultater: Set<VilkårResultat>,
    ): Map<Vilkår, List<Periode<VilkårResultat>>> {
        val vilkårResultaterForAktørMap =
            vilkårResultater
                .filter { it.resultat != Resultat.IKKE_VURDERT }
                .groupByTo(mutableMapOf()) { it.vilkårType }
                .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

        return vilkårResultaterForAktørMap.mapValues {
            forskyvBasertPåVilkårtype(
                vilkårType = it.key,
                alleVilkårResultater = vilkårResultater.filter { it.resultat != Resultat.IKKE_VURDERT }.toList(),
            ).tilTidslinje().tilPerioderIkkeNull()
        }
    }

    private fun forskyvBasertPåVilkårtype(
        vilkårType: Vilkår,
        alleVilkårResultater: List<VilkårResultat>,
    ): List<Periode<VilkårResultat>> {
        val vilkårResultatForVilkårType = alleVilkårResultater.filter { it.vilkårType == vilkårType }
        return when (vilkårType) {
            Vilkår.BARNEHAGEPLASS,
            -> forskyvBarnehageplassVilkår(vilkårResultatForVilkårType)

            Vilkår.BOSATT_I_RIKET,
            Vilkår.LOVLIG_OPPHOLD,
            Vilkår.MEDLEMSKAP,
            Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
            Vilkår.BOR_MED_SØKER,
            Vilkår.BARNETS_ALDER,
            -> forskyvStandardVilkår(vilkårResultatForVilkårType)
        }
    }

    private fun List<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> = if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this
}
