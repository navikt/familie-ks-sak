package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2021.forskyvEtterLovgivning2021
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2024.forskyvEtterLovgivning2024
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.klipp
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

object ForskyvVilkårFørFebruar2025 {
    fun forskyvVilkårResultater(
        vilkårType: Vilkår,
        alleVilkårResultater: List<VilkårResultat>,
    ): List<Periode<VilkårResultat>> {
        val forskjøvetVilkårResultaterTidslinje2021 = forskyvEtterLovgivning2021(vilkårType, alleVilkårResultater).tilTidslinje()

        val forskjøvetVilkårResultaterTidslinje2024 = forskyvEtterLovgivning2024(vilkårType, alleVilkårResultater).tilTidslinje()

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
}
