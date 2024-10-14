package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.tilAndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.regelverk.Regelverk
import org.springframework.stereotype.Component

interface AndelGenerator {
    val regelverk: Regelverk

    fun beregnAndelerForBarn(
        søker: Person,
        barn: Person,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelse>

    companion object {
        fun kombinerForskjøvedeTidslinjerTilOppfyltTidslinje(
            søkersVilkårResultaterForskjøvetTidslinje: Tidslinje<List<VilkårResultat>>,
            barnetsVilkårResultaterForskjøvetTidslinje: Tidslinje<List<VilkårResultat>>,
        ) =
            barnetsVilkårResultaterForskjøvetTidslinje.kombinerMed(
                søkersVilkårResultaterForskjøvetTidslinje,
            ) { barnPeriode, søkerPeriode ->
                søkerPeriode?.let { barnPeriode }
            }

        fun lagAndelerTilkjentYtelse(
            oppfyltTidslinje: Tidslinje<List<VilkårResultat>>,
            tilkjentYtelse: TilkjentYtelse,
            barnAktør: Aktør,
        ) =
            oppfyltTidslinje
                .tilPerioderIkkeNull()
                .map { vilkårResultaterPeriode ->
                    vilkårResultaterPeriode.tilAndelTilkjentYtelse(
                        tilkjentYtelse = tilkjentYtelse,
                        barnAktør = barnAktør,
                    )
                }
    }

    @Component
    class Lookup(
        private val andelGeneratorer: List<AndelGenerator>,
    ) {
        fun hentGeneratorForRegelverk(regelverk: Regelverk) = andelGeneratorer.single { it.regelverk == regelverk }
    }
}
