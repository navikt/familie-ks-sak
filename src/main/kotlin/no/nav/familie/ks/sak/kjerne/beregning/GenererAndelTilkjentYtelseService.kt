package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.RegelverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.gammel.RegelverkFørFebruar2025AndelGeneratorGammel
import no.nav.familie.ks.sak.kjerne.beregning.regelverkLovendringFebruar2025.RegelverkLovendringFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.regelverk.Regelverk
import no.nav.familie.ks.sak.kjerne.regelverk.RegelverkUtleder
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class GenererAndelTilkjentYtelseService(
    private val regelverkLovendringFebruar2025AndelGenerator: RegelverkLovendringFebruar2025AndelGenerator,
    private val regelverkFørFebruar2025AndelGenerator: RegelverkFørFebruar2025AndelGenerator,
    private val unleashService: UnleashService,
) {
    fun genererAndelerTilkjentYtelse(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> =
        if (unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false)) {
            personopplysningGrunnlag.barna.flatMap { barn ->
                genererAndelerForBarn(
                    søker = personopplysningGrunnlag.søker,
                    barn = barn,
                    vilkårsvurdering = vilkårsvurdering,
                    tilkjentYtelse = tilkjentYtelse,
                )
            }
        } else {
            RegelverkFørFebruar2025AndelGeneratorGammel.beregnAndelerTilkjentYtelseForBarna(personopplysningGrunnlag = personopplysningGrunnlag, vilkårsvurdering = vilkårsvurdering, tilkjentYtelse = tilkjentYtelse)
        }

    private fun genererAndelerForBarn(
        søker: Person,
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        val regelverk = RegelverkUtleder.utledRegelverkForBarn(fødselsdato = barn.fødselsdato)
        val andeler =
            when (regelverk) {
                Regelverk.LOVENDRING_FEBRUAR_2025 -> regelverkLovendringFebruar2025AndelGenerator.genererAndelerForBarn(søker = søker, barn = barn, vilkårsvurdering = vilkårsvurdering, tilkjentYtelse = tilkjentYtelse)
                Regelverk.FØR_LOVENDRING_2025 -> regelverkFørFebruar2025AndelGenerator.genererAndelerForBarn(søker = søker, barn = barn, vilkårsvurdering = vilkårsvurdering, tilkjentYtelse = tilkjentYtelse)
            }
        return andeler
    }
}
