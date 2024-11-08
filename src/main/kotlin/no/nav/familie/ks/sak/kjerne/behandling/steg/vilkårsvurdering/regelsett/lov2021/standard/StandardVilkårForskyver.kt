package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.standard

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.mapTilTilknyttetVilkårResultater

private val VILKÅR_SOM_IKKE_SKAL_FORSKYVES_ETTER_STANDARD_LOGIKK =
    setOf(
        Vilkår.BARNEHAGEPLASS,
    )

fun forskyvStandardVilkår(
    vilkårResultater: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {
    val vilkårTyper = vilkårResultater.map { it.vilkårType }.distinct()
    if (vilkårTyper.any { VILKÅR_SOM_IKKE_SKAL_FORSKYVES_ETTER_STANDARD_LOGIKK.contains(it) }) {
        throw IllegalArgumentException("Vilkårtype skal ikke forskyves etter standard logikk")
    }
    return vilkårResultater
        .filter { it.erOppfylt() || it.erIkkeAktuelt() }
        .sortedBy { it.periodeFom }
        .mapTilTilknyttetVilkårResultater()
        .map { Periode(verdi = it.gjeldende, fom = forskyvFom(it.gjeldende.periodeFom), tom = forskyvTom(it)) }
        .filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
}
