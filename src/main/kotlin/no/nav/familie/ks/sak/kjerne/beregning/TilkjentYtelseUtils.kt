package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.SatsPeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import tilFørskjøvetVilkårResultatTidslinjeForPerson
import java.math.RoundingMode
import java.time.LocalDate

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList()
    ): TilkjentYtelse {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = vilkårsvurdering.behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )
        val endretUtbetalingAndelerBarna = endretUtbetalingAndeler.filter { it.person?.type == PersonType.BARN }

        val andelerTilkjentYtelseBarnaUtenEndringer = beregnAndelerTilkjentYtelseForBarna(
            personopplysningGrunnlag = personopplysningGrunnlag,
            vilkårsvurdering = vilkårsvurdering,
            tilkjentYtelse = tilkjentYtelse
        )

        val andelerTilkjentYtelseBarnaMedAlleEndringer = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
            endretUtbetalingAndeler = endretUtbetalingAndelerBarna
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel })
        return tilkjentYtelse
    }

    fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse
    ): List<AndelTilkjentYtelse> {
        val søkersVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilFørskjøvetVilkårResultatTidslinjeForPerson(personopplysningGrunnlag.søker)

        return personopplysningGrunnlag.barna.flatMap { barn ->
            val barnetsVilkårResultaterForskjøvetTidslinje =
                vilkårsvurdering.personResultater.tilFørskjøvetVilkårResultatTidslinjeForPerson(barn)

            val barnVilkårResultaterForskjøvetBådeBarnOgSøkerHarAlleOppfylt =
                barnetsVilkårResultaterForskjøvetTidslinje.kombinerMed(søkersVilkårResultaterForskjøvetTidslinje) { barnPeriode, søkerPeriode ->
                    søkerPeriode?.let { barnPeriode }
                }

            barnVilkårResultaterForskjøvetBådeBarnOgSøkerHarAlleOppfylt
                .tilPerioderIkkeNull()
                .map { vilkårResultaterPeriode ->
                    vilkårResultaterPeriode.tilAndelTilkjentYtelse(
                        vilkårsvurdering = vilkårsvurdering,
                        tilkjentYtelse = tilkjentYtelse,
                        barn = barn
                    )
                }
        }
    }

    private fun Periode<List<VilkårResultat>>.tilAndelTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
        barn: Person
    ): AndelTilkjentYtelse {
        val erDeltBosted = this.verdi.any {
            it.vilkårType == Vilkår.BOR_MED_SØKER &&
                it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
        }

        val antallTimer = this.verdi.single { it.vilkårType == Vilkår.BARNEHAGEPLASS }.antallTimer

        val satsperiode = hentGyldigSatsFor(
            antallTimer = antallTimer?.setScale(2, RoundingMode.HALF_UP),
            erDeltBosted = erDeltBosted,
            stønadFom = fom!!.toYearMonth(),
            stønadTom = tom!!.toYearMonth()
        )

        validerBeregnetPeriode(beløpsperiode = satsperiode, behandlingId = vilkårsvurdering.behandling.id)

        val kalkulertUtbetalingsbeløp = satsperiode.sats.prosent(satsperiode.prosent)

        return AndelTilkjentYtelse(
            behandlingId = vilkårsvurdering.behandling.id,
            tilkjentYtelse = tilkjentYtelse,
            aktør = barn.aktør,
            stønadFom = satsperiode.fom,
            stønadTom = satsperiode.tom,
            kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
            nasjonaltPeriodebeløp = kalkulertUtbetalingsbeløp,
            type = YtelseType.ORDINÆR_KONTANTSTØTTE,
            sats = satsperiode.sats,
            prosent = satsperiode.prosent
        )
    }

    private fun validerBeregnetPeriode(beløpsperiode: SatsPeriode, behandlingId: Long) {
        if (beløpsperiode.fom.isAfter(beløpsperiode.tom)) {
            throw Feil(
                "Feil i beregning for behandling $behandlingId," +
                    "fom ${beløpsperiode.fom} kan ikke være større enn tom ${beløpsperiode.tom}"
            )
        }
    }

    private fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
        andelTilkjentYtelserUtenEndringer: List<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer.map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it) }
        }
        // TODO - Endret Utbetaling Andel implementasjon kommer i neste levering
        return emptyList()
    }
}
