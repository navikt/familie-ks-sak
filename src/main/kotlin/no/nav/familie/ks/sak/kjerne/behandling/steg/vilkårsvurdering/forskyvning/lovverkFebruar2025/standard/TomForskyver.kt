package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.standard

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.TilknyttetVilkårResultater
import java.time.LocalDate

fun forskyvTom(
    tilknyttetVilkårResultater: TilknyttetVilkårResultater,
): LocalDate? {
    val periodeTom = tilknyttetVilkårResultater.gjeldende.periodeTom ?: return null
    return when {
        tilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste() -> periodeTom.plusDays(1).sisteDagIMåned()
        else -> periodeTom.minusMonths(1).sisteDagIMåned()
    }
}
