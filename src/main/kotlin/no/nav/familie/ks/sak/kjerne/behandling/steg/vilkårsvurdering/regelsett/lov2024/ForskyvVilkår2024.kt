package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.tilVilkårResultaterMedInformasjonOmNestePeriode

fun forskyvEtterLovgivning2024(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>,
) = when (vilkårType) {
    Vilkår.BARNEHAGEPLASS -> vilkårResultater.forskyvBarnehageplassVilkår2024()

    else ->
        vilkårResultater.filter { it.erOppfylt() || it.erIkkeAktuelt() }.sortedBy { it.periodeFom }.tilVilkårResultaterMedInformasjonOmNestePeriode()
            .map {
                val forskjøvetTom =
                    when {
                        it.slutterDagenFørNeste -> {
                            it.vilkårResultat.periodeTom
                        }

                        else -> it.vilkårResultat.periodeTom?.sisteDagIMåned()
                    }

                Periode(
                    verdi = it.vilkårResultat,
                    fom = it.vilkårResultat.periodeFom?.førsteDagIInneværendeMåned(),
                    tom = forskjøvetTom,
                )
            }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
}
