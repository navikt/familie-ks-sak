package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.standard

import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.mapTilTilknyttetVilkårResultater
import no.nav.familie.tidslinje.Periode

private val VILKÅR_SOM_IKKE_SKAL_FORSKYVES_ETTER_STANDARD_LOGIKK =
    setOf(
        Vilkår.BARNEHAGEPLASS,
    )

fun forskyvStandardVilkår(
    vilkårResultater: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {
    validerVilkårResultater(vilkårResultater)
    return vilkårResultater
        .filter { it.erOppfylt() || it.erIkkeAktuelt() }
        .sortedBy { it.periodeFom }
        .mapTilTilknyttetVilkårResultater()
        .map { Periode(verdi = it.gjeldende, fom = forskyvFom(it.gjeldende.periodeFom), tom = forskyvTom(it)) }
        .filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
}

private fun validerVilkårResultater(vilkårResultater: List<VilkårResultat>) {
    vilkårResultater.map { it.vilkårType }.distinct().forEach {
        if (it in VILKÅR_SOM_IKKE_SKAL_FORSKYVES_ETTER_STANDARD_LOGIKK) {
            throw IllegalArgumentException("Vilkårtype $it skal ikke forskyves etter standard logikk")
        }
    }
}
