package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.gammel.RegelverkFørFebruar2025AndelGeneratorGammel
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilkjentYtelseService {
    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
    ): TilkjentYtelse {
        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            )
        val endretUtbetalingAndelerBarna = endretUtbetalingAndeler.filter { it.person?.type == PersonType.BARN }

        // TODO start
        //  Her må vi kalle på en "Overordnet"-service som sjekker feature-toggle og som kjører gammel løype hvis toggle er av og ny løype dersom toggle er på. RegelverkFørFebruar2025AndelGeneratorGammel, representerer den gamle løypa.
        val andelerTilkjentYtelseBarnaUtenEndringer =
            RegelverkFørFebruar2025AndelGeneratorGammel.beregnAndelerTilkjentYtelseForBarna(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )
        // TODO end

        val andelerTilkjentYtelseBarnaMedAlleEndringer =
            AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel })
        return tilkjentYtelse
    }
}
