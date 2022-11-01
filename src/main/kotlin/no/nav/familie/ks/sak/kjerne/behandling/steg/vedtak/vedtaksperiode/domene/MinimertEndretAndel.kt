package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.NullableMånedPeriode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.beregning.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.beregning.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

class MinimertEndretAndel(
    val aktørId: String,
    val fom: YearMonth?,
    val tom: YearMonth?,
    val årsak: Årsak?,
    val prosent: BigDecimal?
) {
    fun månedPeriode() = MånedPeriode(fom!!, tom!!)

    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        if (nullableMånedPeriode.fom == null) {
            throw Feil("Fom ble null ved sjekk av overlapp av periode til endretUtbetalingAndel")
        }

        return MånedPeriode(
            this.fom!!,
            this.tom!!
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom,
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth()
            )
        )
    }
}

fun EndretUtbetalingAndel.tilMinimertEndretUtbetalingAndel(): MinimertEndretAndel {
    this.validerUtfyltEndring()

    return MinimertEndretAndel(
        fom = this.fom!!,
        tom = this.tom!!,
        aktørId = this.person?.aktør?.aktørId ?: throw Feil(
            "Finner ikke aktørId på endretUtbetalingsandel ${this.id} " +
                "ved konvertering til minimertEndretUtbetalingsandel"
        ),
        årsak = this.årsak ?: throw Feil(
            "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
                "ved konvertering til minimertEndretUtbetalingsandel"
        ),
        prosent = this.prosent
    )
}
