package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkEtterFebruar2025

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkEtterFebruar2025.lov2025.forskyvEtterLovgivning2025
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

object ForskyvVilkårEtterFebruar2025 {
    fun forskyvVilkårResultater(
        vilkårResultater: List<VilkårResultat>,
    ): Map<Vilkår, List<Periode<VilkårResultat>>> {
        val vilkårResultaterForAktørMap =
            vilkårResultater
                .filter { it.resultat != Resultat.IKKE_VURDERT }
                .groupByTo(mutableMapOf()) { it.vilkårType }
                .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

        return vilkårResultaterForAktørMap.mapValues {
            forskyvEtterLovgivning2025(
                vilkårType = it.key,
                alleVilkårResultater = vilkårResultater.filter { it.resultat != Resultat.IKKE_VURDERT }.toList(),
            ).tilTidslinje().tilPerioderIkkeNull()
        }
    }

    private fun List<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
        if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this
}
