package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.TilknyttetVilkårResultater
import java.time.LocalDate

fun forskyvTom(
    TilknyttetVilkårResultater: TilknyttetVilkårResultater,
): LocalDate? {
    val gjeldendePeriodeTom = TilknyttetVilkårResultater.gjeldende.periodeTom
    return when {
        gjeldendePeriodeTom == null -> null
        TilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste() -> gjeldendePeriodeTom
        else -> gjeldendePeriodeTom.sisteDagIMåned()
    }
}
