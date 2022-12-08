package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.NullableMånedPeriode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder personIdent og ikke aktørId.
 */
data class BrevEndretAndel(
    val periode: MånedPeriode,
    val personIdent: String,
    val årsak: Årsak,
    val søknadstidspunkt: LocalDate,
    val avtaletidspunktDeltBosted: LocalDate?
) {
    fun erOverlappendeMed(nullableMånedPeriode: NullableMånedPeriode): Boolean {
        return MånedPeriode(
            this.periode.fom,
            this.periode.tom
        ).overlapperHeltEllerDelvisMed(
            MånedPeriode(
                nullableMånedPeriode.fom ?: TIDENES_MORGEN.toYearMonth(),
                nullableMånedPeriode.tom ?: TIDENES_ENDE.toYearMonth()
            )
        )
    }
}

fun List<BrevEndretAndel>.somOverlapper(nullableMånedPeriode: NullableMånedPeriode) =
    this.filter { it.erOverlappendeMed(nullableMånedPeriode) }

fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.tilMinimertRestEndretUtbetalingAndel() = BrevEndretAndel(
    periode = this.periode,
    personIdent = this.person?.aktør?.aktivFødselsnummer() ?: throw Feil(
        "Har ikke ident på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertRestEndretUtbetalingsandel"
    ),
    årsak = this.årsak ?: throw Feil(
        "Har ikke årsak på endretUtbetalingsandel ${this.id} " +
            "ved konvertering til minimertRestEndretUtbetalingsandel"
    ),
    søknadstidspunkt = this.søknadstidspunkt ?: throw Feil(
        "Har ikke søknadstidspunk på endretUtbetalingsandel  ${this.id} " +
            "ved konvertering til minimertRestEndretUtbetalingsandel"
    ),
    avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted ?: (
        if (this.årsakErDeltBosted()) {
            throw Feil(
                "Har ikke avtaletidspunktDeltBosted på endretUtbetalingsandel  ${this.id} " +
                    "ved konvertering til minimertRestEndretUtbetalingsandel"
            )
        } else {
            null
        }
        )
)
