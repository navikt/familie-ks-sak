package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.TilknyttetVilkårResultater
import java.time.LocalDate

fun forskyvTom(
    tilknyttetVilkårResultater: TilknyttetVilkårResultater,
): LocalDate? {
    val gjeldendePeriodeTom = tilknyttetVilkårResultater.gjeldende.periodeTom
    return when {
        gjeldendePeriodeTom == null -> null
        tilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste() -> gjeldendePeriodeTom
        else -> gjeldendePeriodeTom.sisteDagIMåned()
    }
}
