package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.standard

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.TilknyttetVilkårResultater
import java.time.LocalDate

private val sisteMuligeTomForBarnetsAlderILov2021 = DATO_LOVENDRING_2024.minusDays(1)

fun forskyvTom(
    tilknyttetVilkårResultater: TilknyttetVilkårResultater,
): LocalDate? {
    val periodeTom = tilknyttetVilkårResultater.gjeldende.periodeTom
    if (periodeTom == null) {
        return null
    }
    val skalHaUtbetaltForJuli2024PgaLovendring = finnSkalHaUtbetaltForJuli2024PgaLovendring(tilknyttetVilkårResultater.gjeldende)
    return when {
        tilknyttetVilkårResultater.gjeldendeSlutterDagenFørNeste() -> periodeTom.plusDays(1).sisteDagIMåned()
        skalHaUtbetaltForJuli2024PgaLovendring -> periodeTom
        else -> periodeTom.minusMonths(1).sisteDagIMåned()
    }
}

private fun finnSkalHaUtbetaltForJuli2024PgaLovendring(gjeldendeVilkårResultat: VilkårResultat): Boolean {
    val periodeFom = gjeldendeVilkårResultat.periodeFom
    val periodeTom = gjeldendeVilkårResultat.periodeTom
    val vilkårType = gjeldendeVilkårResultat.vilkårType
    return when {
        vilkårType != Vilkår.BARNETS_ALDER -> false
        periodeTom != sisteMuligeTomForBarnetsAlderILov2021 -> false
        periodeFom?.plusYears(1) == sisteMuligeTomForBarnetsAlderILov2021 -> false
        else -> true
    }
}
