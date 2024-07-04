package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat

data class TilknyttetVilkårResultater(
    val gjeldende: VilkårResultat,
    val neste: VilkårResultat?,
) {
    fun gjeldendeSlutterDagenFørNeste(): Boolean {
        if (neste == null) {
            return false
        }
        return gjeldende.periodeTom?.erDagenFør(neste.periodeFom) ?: false
    }
}
