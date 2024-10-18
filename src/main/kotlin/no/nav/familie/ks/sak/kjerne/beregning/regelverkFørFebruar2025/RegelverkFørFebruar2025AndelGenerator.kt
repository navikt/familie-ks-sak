package no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator.Companion.kombinerForskjøvedeTidslinjerTilOppfyltTidslinje
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator.Companion.lagAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.regelverk.Regelverk
import org.springframework.stereotype.Component

@Component
class RegelverkFørFebruar2025AndelGenerator : AndelGenerator {
    override val regelverk = Regelverk.FØR_LOVENDRING_2025

    override fun beregnAndelerForBarn(
        søker: Person,
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        val søkersVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
                søker,
            )

        val barnetsVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(barn)

        val oppfyltTidslinje = kombinerForskjøvedeTidslinjerTilOppfyltTidslinje(søkersVilkårResultaterForskjøvetTidslinje, barnetsVilkårResultaterForskjøvetTidslinje)

        return lagAndelerTilkjentYtelse(
            oppfyltTidslinje = oppfyltTidslinje,
            tilkjentYtelse = tilkjentYtelse,
            barnAktør = barn.aktør,
        )
    }
}
