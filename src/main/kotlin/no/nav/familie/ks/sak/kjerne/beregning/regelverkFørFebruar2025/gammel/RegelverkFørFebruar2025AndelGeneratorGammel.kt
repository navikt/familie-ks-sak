package no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.gammel

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.SatsPeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.springframework.stereotype.Service
import java.math.RoundingMode

@Service
class RegelverkFørFebruar2025AndelGeneratorGammel {
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

    private fun Periode<List<VilkårResultat>>.tilAndelTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
        barn: Person,
    ): AndelTilkjentYtelse {
        val erDeltBosted =
            this.verdi.any {
                it.vilkårType == Vilkår.BOR_MED_SØKER &&
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
            }

        val antallTimer = this.verdi.single { it.vilkårType == Vilkår.BARNEHAGEPLASS }.antallTimer

        val satsperiode =
            hentGyldigSatsFor(
                antallTimer = antallTimer?.setScale(2, RoundingMode.HALF_UP),
                erDeltBosted = erDeltBosted,
                stønadFom = fom!!.toYearMonth(),
                stønadTom = tom!!.toYearMonth(),
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
            prosent = satsperiode.prosent,
        )
    }

    private fun validerBeregnetPeriode(
        beløpsperiode: SatsPeriode,
        behandlingId: Long,
    ) {
        if (beløpsperiode.fom.isAfter(beløpsperiode.tom)) {
            throw Feil(
                "Feil i beregning for behandling $behandlingId," +
                    "fom ${beløpsperiode.fom} kan ikke være større enn tom ${beløpsperiode.tom}",
            )
        }
    }
}
