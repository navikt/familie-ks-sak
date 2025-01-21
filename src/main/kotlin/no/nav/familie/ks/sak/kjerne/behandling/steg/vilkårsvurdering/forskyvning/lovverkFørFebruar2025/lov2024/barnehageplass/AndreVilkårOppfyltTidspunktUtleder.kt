package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.alleVilkårOppfyltEllerNull
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.standard.forskyvStandardVilkår2024
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.YearMonth

fun utledTidligsteÅrMånedAlleAndreVilkårErOppfylt(
    andreVilkårResultater: List<VilkårResultat>,
): YearMonth? {
    val vilkårResultatPerType = andreVilkårResultater.groupBy { it.vilkårType }
    if (vilkårResultatPerType.containsKey(Vilkår.BARNEHAGEPLASS)) {
        throw IllegalArgumentException("Fant vilkår barnehageplass men forventent at det ikke skulle bli sendt inn")
    }
    return vilkårResultatPerType
        .map { forskyvStandardVilkår2024(it.key, it.value) }
        .map { it.tilTidslinje() }
        .kombiner {
            alleVilkårOppfyltEllerNull(
                vilkårResultater = it,
                personType = PersonType.BARN,
                vilkårSomIkkeSkalSjekkesPå = listOf(Vilkår.BARNEHAGEPLASS),
            )
        }.tilPerioderIkkeNull()
        .mapNotNull { it.fom }
        .map { it.toYearMonth() }
        .minOfOrNull { it }
        ?.let { månedAlleAndreVilkårErOppfylt ->
            // Før lovendring 2024 så fikk man ikke innvilget kontantstøtte før måneden etter
            if (månedAlleAndreVilkårErOppfylt < DATO_LOVENDRING_2024.toYearMonth()) {
                månedAlleAndreVilkårErOppfylt.plusMonths(1)
            } else {
                månedAlleAndreVilkårErOppfylt
            }
        }
}
