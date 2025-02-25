package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat

data class BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<NullableVilkårResultat : VilkårResultat?>(
    val vilkårResultat: NullableVilkårResultat,
    val graderingsforskjellMellomDenneOgForrigePeriode: Graderingsforskjell,
    val graderingsforskjellMellomDenneOgNestePeriode: Graderingsforskjell,
)

@Suppress("UNCHECKED_CAST")
fun List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat?>>.filtrerBortNullverdier(): List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>> = this.filter { it.vilkårResultat != null } as List<BarnehageplassVilkårMedGraderingsforskjellMellomPerioder<VilkårResultat>>
