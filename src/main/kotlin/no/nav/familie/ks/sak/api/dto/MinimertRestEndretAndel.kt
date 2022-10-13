package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder personIdent og ikke aktørId.
 */
data class MinimertRestEndretAndel(
    val periode: MånedPeriode,
    val personIdent: String,
    val årsak: Årsak,
    val søknadstidspunkt: LocalDate,
    val avtaletidspunktDeltBosted: LocalDate?
)

fun EndretUtbetalingAndelMedAndelerTilkjentYtelse.tilMinimertRestEndretUtbetalingAndel() = MinimertRestEndretAndel(
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
