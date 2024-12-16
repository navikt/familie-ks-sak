package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.barnehageplass.forskyvBarnehageplassVilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.standard.forskyvStandardVilkår
import no.nav.familie.tidslinje.Periode

fun forskyvEtterLovgivning2021(
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
