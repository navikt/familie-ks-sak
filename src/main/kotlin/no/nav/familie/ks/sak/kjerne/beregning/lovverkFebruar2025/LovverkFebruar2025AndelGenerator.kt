package no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator.Companion.kombinerForskjøvedeTidslinjerTilOppfyltTidslinje
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator.Companion.lagAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
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
        val søkersVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
                person = søker,
                lovverk = lovverk,
            )

        val barnetsVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
                person = barn,
                lovverk = lovverk,
            )

        val oppfyltTidslinje = kombinerForskjøvedeTidslinjerTilOppfyltTidslinje(søkersVilkårResultaterForskjøvetTidslinje, barnetsVilkårResultaterForskjøvetTidslinje)

        return lagAndelerTilkjentYtelse(
            oppfyltTidslinje = oppfyltTidslinje,
            tilkjentYtelse = tilkjentYtelse,
            barnAktør = barn.aktør,
        )
    }
}
