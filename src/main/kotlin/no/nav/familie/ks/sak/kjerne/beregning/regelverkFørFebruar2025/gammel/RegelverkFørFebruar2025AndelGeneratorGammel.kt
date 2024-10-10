package no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.gammel

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.tilAndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

object RegelverkFørFebruar2025AndelGeneratorGammel {
    @Deprecated(message = "Skal slettes")
    fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        val søkersVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
                personopplysningGrunnlag.søker,
            )

        return personopplysningGrunnlag.barna.flatMap { barn ->
            val barnetsVilkårResultaterForskjøvetTidslinje =
                vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(barn)

            val barnVilkårResultaterForskjøvetBådeBarnOgSøkerHarAlleOppfylt =
                barnetsVilkårResultaterForskjøvetTidslinje.kombinerMed(
                    søkersVilkårResultaterForskjøvetTidslinje,
                ) { barnPeriode, søkerPeriode ->
                    søkerPeriode?.let { barnPeriode }
                }

            barnVilkårResultaterForskjøvetBådeBarnOgSøkerHarAlleOppfylt
                .tilPerioderIkkeNull()
                .map { vilkårResultaterPeriode ->
                    vilkårResultaterPeriode.tilAndelTilkjentYtelse(
                        vilkårsvurdering = vilkårsvurdering,
                        tilkjentYtelse = tilkjentYtelse,
                        barn = barn,
                    )
                }
        }
    }
}
