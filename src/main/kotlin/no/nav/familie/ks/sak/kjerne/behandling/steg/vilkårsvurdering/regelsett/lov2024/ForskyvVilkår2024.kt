package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass.forskyvBarnehageplassVilkår2024

fun forskyvEtterLovgivning2024(
    vilkårType: Vilkår,
    alleVilkårResultater: List<VilkårResultat>,
) = when (vilkårType) {
    Vilkår.BARNEHAGEPLASS,
    -> forskyvBarnehageplassVilkår2024(alleVilkårResultater)

    Vilkår.BOSATT_I_RIKET,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.MEDLEMSKAP,
    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
    Vilkår.BOR_MED_SØKER,
    Vilkår.BARNETS_ALDER,
    -> forskyvAndreVilkår2024(vilkårType, alleVilkårResultater)
}
