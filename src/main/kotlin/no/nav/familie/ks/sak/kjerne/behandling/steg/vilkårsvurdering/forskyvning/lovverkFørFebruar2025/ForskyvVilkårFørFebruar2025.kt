package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2021.forskyvEtterLovgivning2021
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.forskyvEtterLovgivning2024
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.klipp
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

object ForskyvVilkårFørFebruar2025 {
    fun forskyvVilkårResultater(
        vilkårResultater: Set<VilkårResultat>,
    ): Map<Vilkår, List<Periode<VilkårResultat>>> {
        val vilkårResultaterForAktørMap =
            vilkårResultater
                .filter { it.resultat != Resultat.IKKE_VURDERT }
                .groupByTo(mutableMapOf()) { it.vilkårType }
                .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

        return vilkårResultaterForAktørMap.mapValues {
            forskyvVilkår(vilkårType = it.key, alleVilkårResultaterForPerson = vilkårResultater.filter { it.resultat != Resultat.IKKE_VURDERT }.toList())
        }
    }

    private fun forskyvVilkår(
        vilkårType: Vilkår,
        alleVilkårResultaterForPerson: List<VilkårResultat>,
    ): List<Periode<VilkårResultat>> {
        val forskjøvetVilkårResultaterTidslinje2021 = forskyvEtterLovgivning2021(vilkårType, alleVilkårResultaterForPerson).tilTidslinje()

        val forskjøvetVilkårResultaterTidslinje2024 = forskyvEtterLovgivning2024(vilkårType, alleVilkårResultaterForPerson).tilTidslinje()

        val klippetTidslinje2021 =
            forskjøvetVilkårResultaterTidslinje2021.klipp(
                startsTidspunkt = forskjøvetVilkårResultaterTidslinje2021.startsTidspunkt,
                sluttTidspunkt = DATO_LOVENDRING_2024.minusDays(1),
            )

        val klippetTidslinje2024 =
            forskjøvetVilkårResultaterTidslinje2024.klipp(
                startsTidspunkt = DATO_LOVENDRING_2024,
                sluttTidspunkt = TIDENES_ENDE,
            )

        return klippetTidslinje2021
            .kombinerMed(klippetTidslinje2024) { vilkår2021, vilkår2024 -> vilkår2021 ?: vilkår2024 }
            .tilPerioderIkkeNull()
    }

    private fun List<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> = if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this
}
