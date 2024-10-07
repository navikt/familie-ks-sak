package no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import org.springframework.stereotype.Component

@Component
class RegelverkFørFebruar2025AndelGenerator {
    fun genererAndelerForBarn(
        søker: Person,
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        return emptyList()
    }
}
