package no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.regelverk.Lovverk
import org.springframework.stereotype.Component

@Component
class LovverkFebruar2025AndelGenerator : AndelGenerator {
    override val lovverk = Lovverk.LOVENDRING_FEBRUAR_2025

    override fun beregnAndelerForBarn(
        søker: Person,
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        // TODO: Skrive logikk for å generere andeler etter nytt regelverk
        return emptyList()
    }
}
