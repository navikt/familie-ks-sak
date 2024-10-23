package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.alleVilkårOppfyltEllerNull
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.forskyvStandardVilkår2024
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.time.YearMonth

fun utledTidligsteÅrMånedAlleAndreVilkårErOppfylt(
    andreVilkårResultater: List<VilkårResultat>,
): YearMonth? =
    andreVilkårResultater
        .groupBy { it.vilkårType }
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
