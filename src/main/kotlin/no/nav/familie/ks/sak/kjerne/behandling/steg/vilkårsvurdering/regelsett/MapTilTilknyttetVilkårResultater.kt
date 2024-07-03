package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat

fun List<VilkårResultat>.mapTilTilknyttetVilkårResultater(): List<TilknyttetVilkårResultater> {
    if (this.isEmpty()) {
        return emptyList()
    }
    this.sortedBy { it.periodeFom }
    val zipped = this.zipWithNext { denne, neste -> TilknyttetVilkårResultater(denne, neste) }
    return zipped + TilknyttetVilkårResultater(last(), null)
}
