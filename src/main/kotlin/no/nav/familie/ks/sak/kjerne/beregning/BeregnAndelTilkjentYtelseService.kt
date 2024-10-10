package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.regelverk.RegelverkUtleder
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service

@Service
class BeregnAndelTilkjentYtelseService(
    private val andelGeneratorLookup: AndelGenerator.Lookup,
    private val unleashService: UnleashService,
) {
    fun beregnAndelerTilkjentYtelse(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> =
        personopplysningGrunnlag.barna.flatMap { barn ->
            beregnAndelerForBarn(
                søker = personopplysningGrunnlag.søker,
                barn = barn,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )
        }

    private fun beregnAndelerForBarn(
        søker: Person,
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse> {
        val regelverk =
            RegelverkUtleder.utledRegelverkForBarn(
                fødselsdato = barn.fødselsdato,
                skalBestemmeRegelverkBasertPåFødselsdato = unleashService.isEnabled(FeatureToggleConfig.BRUK_NY_LØYPE_FOR_GENERERING_AV_ANDELER, false),
            )
        val andelGenerator = andelGeneratorLookup.hentGeneratorForRegelverk(regelverk)
        val andeler = andelGenerator.beregnAndelerForBarn(søker = søker, barn = barn, vilkårsvurdering = vilkårsvurdering, tilkjentYtelse = tilkjentYtelse)
        return andeler
    }
}
