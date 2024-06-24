package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat

data class VilkårResultaterMedInformasjonOmNestePeriode(
    val vilkårResultat: VilkårResultat,
    val slutterDagenFørNeste: Boolean,
)

fun List<VilkårResultat>.tilVilkårResultaterMedInformasjonOmNestePeriode(): List<VilkårResultaterMedInformasjonOmNestePeriode> {
    if (isEmpty()) {
        return emptyList()
    }
    return zipWithNext { denne, neste ->
        VilkårResultaterMedInformasjonOmNestePeriode(
            vilkårResultat = denne,
            slutterDagenFørNeste = denne.periodeTom?.erDagenFør(neste.periodeFom) ?: false,
        )
    } + VilkårResultaterMedInformasjonOmNestePeriode(last(), false)
}
