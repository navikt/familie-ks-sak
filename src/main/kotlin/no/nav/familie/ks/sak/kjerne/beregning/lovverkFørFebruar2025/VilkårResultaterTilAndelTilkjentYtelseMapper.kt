package no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.SatsPeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import java.math.RoundingMode

fun Periode<List<VilkårResultat>>.tilAndelTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse,
    barnAktør: Aktør,
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

    validerBeregnetPeriode(beløpsperiode = satsperiode, behandlingId = tilkjentYtelse.behandling.id)

    val kalkulertUtbetalingsbeløp = satsperiode.sats.prosent(satsperiode.prosent)

    return AndelTilkjentYtelse(
        behandlingId = tilkjentYtelse.behandling.id,
        tilkjentYtelse = tilkjentYtelse,
        aktør = barnAktør,
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
