package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service

@Service
class BeregnAndelTilkjentYtelseService(
    private val andelGeneratorLookup: AndelGenerator.Lookup,
    private val unleashService: UnleashNextMedContextService,
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
            LovverkUtleder.utledLovverkForBarn(
                fødselsdato = barn.fødselsdato,
                skalBestemmeLovverkBasertPåFødselsdato = unleashService.isEnabled(FeatureToggle.STØTTER_LOVENDRING_2025),
            )
        val andelGenerator = andelGeneratorLookup.hentGeneratorForLovverk(regelverk)
        val andeler = andelGenerator.beregnAndelerForBarn(søker = søker, barn = barn, vilkårsvurdering = vilkårsvurdering, tilkjentYtelse = tilkjentYtelse)
        return andeler
    }
}
