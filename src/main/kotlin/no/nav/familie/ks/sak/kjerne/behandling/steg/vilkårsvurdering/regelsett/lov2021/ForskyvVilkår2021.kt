package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.mapTilTilknyttetVilkårResultater
import java.time.LocalDate

private val DATO_FOR_LOVENDRING_AV_FORSKYVNINGER: LocalDate = LocalDate.of(2024, 8, 1)

fun forskyvEtterLovgivning2021(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>,
) = when (vilkårType) {
    Vilkår.BARNEHAGEPLASS -> {
        vilkårResultater.forskyvBarnehageplassVilkår()
    }

    Vilkår.BOSATT_I_RIKET,
    Vilkår.LOVLIG_OPPHOLD,
    Vilkår.MEDLEMSKAP,
    Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
    Vilkår.BOR_MED_SØKER,
    Vilkår.BARNETS_ALDER,
    -> {
        vilkårResultater
            .filter { it.erOppfylt() || it.erIkkeAktuelt() }
            .sortedBy { it.periodeFom }
            .mapTilTilknyttetVilkårResultater()
            .map {
                Periode(
                    verdi = it.gjeldende,
                    fom = it.gjeldende.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
                    tom =
                        when {
                            it.gjeldendeSlutterDagenFørNeste() || it.gjeldende.periodeTom == DATO_FOR_LOVENDRING_AV_FORSKYVNINGER.minusDays(1) -> it.gjeldende.periodeTom?.plusDays(1)?.sisteDagIMåned()

                            else -> it.gjeldende.periodeTom?.minusMonths(1)?.sisteDagIMåned()
                        },
                )
            }
            .filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
    }
}
