package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.KompensasjonAndelRepository
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.UtfyltKompensasjonAndel
import no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene.tilIKompensasjonAndel
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilkjentYtelseService(
    private val beregnAndelTilkjentYtelseService: BeregnAndelTilkjentYtelseService,
    private val kompensasjonAndelRepository: KompensasjonAndelRepository,
) {
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

        val andelerTilkjentYtelseBarnaUtenEndringer =
            beregnAndelTilkjentYtelseService.beregnAndelerTilkjentYtelse(personopplysningGrunnlag, vilkårsvurdering, tilkjentYtelse)

        val andelerTilkjentYtelseBarnaMedAlleEndringer =
            AndelTilkjentYtelseMedEndretUtbetalingBehandler.oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
            )

        val kompensasjonAndelerSomAndelTilkjentYtelse =
            kompensasjonAndelRepository
                .hentKompensasjonAndelerForBehandling(vilkårsvurdering.behandling.id)
                .map { it.tilIKompensasjonAndel() }
                .filterIsInstance<UtfyltKompensasjonAndel>()
                .map { kompensasjonAndel ->
                    AndelTilkjentYtelse(
                        behandlingId = vilkårsvurdering.behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = kompensasjonAndel.person.aktør,
                        prosent = kompensasjonAndel.prosent,
                        stønadFom = kompensasjonAndel.fom,
                        stønadTom = kompensasjonAndel.tom,
                        kalkulertUtbetalingsbeløp = maksBeløp().prosent(kompensasjonAndel.prosent),
                        nasjonaltPeriodebeløp = maksBeløp().prosent(kompensasjonAndel.prosent),
                        type = YtelseType.KOMPENSASJONSORDNING_2024,
                        sats = maksBeløp(),
                    )
                }

        val alleAndelerTilkjentYtelse = andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel } + kompensasjonAndelerSomAndelTilkjentYtelse

        tilkjentYtelse.andelerTilkjentYtelse.addAll(alleAndelerTilkjentYtelse)
        return tilkjentYtelse
    }
}
