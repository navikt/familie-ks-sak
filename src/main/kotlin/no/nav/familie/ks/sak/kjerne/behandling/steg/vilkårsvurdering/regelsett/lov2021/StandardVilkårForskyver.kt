package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.mapTilTilknyttetVilkårResultater

fun forskyvStandardVilkår(
    vilkårResultater: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {
    val sisteMuligeTomForBarnetsAlderILov2021 = DATO_LOVENDRING_2024.minusDays(1)
    return vilkårResultater
        .filter { it.erOppfylt() || it.erIkkeAktuelt() }
        .sortedBy { it.periodeFom }
        .mapTilTilknyttetVilkårResultater()
        .map {
            val periodeTom = it.gjeldende.periodeTom

            val skalHaUtbetaltForJuli2024PgaLovendring =
                it.gjeldende.vilkårType == Vilkår.BARNETS_ALDER &&
                    periodeTom == sisteMuligeTomForBarnetsAlderILov2021 &&
                    it.gjeldende.periodeFom?.plusYears(1) != sisteMuligeTomForBarnetsAlderILov2021

            Periode(
                verdi = it.gjeldende,
                fom =
                    it.gjeldende.periodeFom
                        ?.plusMonths(1)
                        ?.førsteDagIInneværendeMåned(),
                tom =
                    when {
                        it.gjeldendeSlutterDagenFørNeste() -> periodeTom?.plusDays(1)?.sisteDagIMåned()
                        skalHaUtbetaltForJuli2024PgaLovendring -> periodeTom
                        else -> periodeTom?.minusMonths(1)?.sisteDagIMåned()
                    },
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
}
