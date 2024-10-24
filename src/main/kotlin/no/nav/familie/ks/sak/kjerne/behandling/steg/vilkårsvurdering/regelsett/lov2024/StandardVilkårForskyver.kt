package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.mapTilTilknyttetVilkårResultater

private val IKKE_STANDARD_VILKÅR =
    setOf(
        Vilkår.BARNEHAGEPLASS,
    )

fun forskyvStandardVilkår2024(
    vilkårType: Vilkår,
    alleVilkårResultater: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {
    if (vilkårType in IKKE_STANDARD_VILKÅR) {
        throw IllegalArgumentException("$vilkårType skal ikke behandles etter standard logikk")
    }
    return alleVilkårResultater
        .filter { it.vilkårType == vilkårType }
        .filter { it.erOppfylt() || it.erIkkeAktuelt() }
        .sortedBy { it.periodeFom }
        .mapTilTilknyttetVilkårResultater()
        .map {
            Periode(
                verdi = it.gjeldende,
                fom = it.gjeldende.periodeFom?.førsteDagIInneværendeMåned(),
                tom =
                    when (it.gjeldendeSlutterDagenFørNeste()) {
                        true -> it.gjeldende.periodeTom
                        false -> it.gjeldende.periodeTom?.sisteDagIMåned()
                    },
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .filtrerBortOverlappendePerioder()
}

private fun List<Periode<VilkårResultat>>.filtrerBortOverlappendePerioder() =
    map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.minByOrNull { it.periodeFom ?: TIDENES_MORGEN } }
        .tilPerioderIkkeNull()
