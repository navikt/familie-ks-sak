package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårRegelsett
import java.time.LocalDate


private val DATO_FOR_LOVENDRING_AV_FORSKYVNINGER: LocalDate = LocalDate.of(2024, 8, 1)

fun utledVilkårRegelsettForDato(dato: LocalDate): VilkårRegelsett =
    when (dato.isBefore(DATO_FOR_LOVENDRING_AV_FORSKYVNINGER)) {
        true -> VilkårRegelsett.LOV_AUGUST_2021
        false -> VilkårRegelsett.LOV_AUGUST_2024
    }
