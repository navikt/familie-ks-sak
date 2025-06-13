package no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.avrundetHeltallAvProsent
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.medEndring
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriode
import no.nav.familie.ks.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.ZipPadding
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.tidslinje.utvidelser.zipMedNeste
import java.math.BigDecimal

object AndelTilkjentYtelseMedEndretUtbetalingBehandler {
    fun lagAndelerMedEndretUtbetalingAndeler(
        andelTilkjentYtelserUtenEndringer: Collection<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer
                .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it.copy()) }
        }

        if (andelTilkjentYtelserUtenEndringer.any { it.type != YtelseType.ORDINÆR_KONTANTSTØTTE }) {
            throw Feil("Kan kun oppdatere ordinære kontantstøtte-andeler med endret utbetaling. Prøver å oppdatere ${andelTilkjentYtelserUtenEndringer.filter { it.type != YtelseType.ORDINÆR_KONTANTSTØTTE}.toSet()}")
        }

        val andelerPerAktør = andelTilkjentYtelserUtenEndringer.groupBy { it.aktør }
        val endringerPerAktør =
            endretUtbetalingAndeler
                .flatMap { andel ->
                    andel.personer
                        .ifEmpty {
                            throw Feil("Endret utbetaling andel ${andel.id} i behandling ${tilkjentYtelse.behandling.id} er ikke knyttet til noen personer")
                        }.map { person ->
                            person.aktør to andel
                        }
                }.groupBy({ it.first }, { it.second })

        val andelerMedEndringer =
            andelerPerAktør.flatMap { (aktør, andelerForAktør) ->
                lagAndelerMedEndretUtbetalingAndelerForPerson(
                    andelerAvTypeForPerson = andelerForAktør,
                    endretUtbetalingAndelerForPerson = endringerPerAktør.getOrDefault(aktør, emptyList()),
                    tilkjentYtelse = tilkjentYtelse,
                )
            }

        return andelerMedEndringer
    }

    private fun lagAndelerMedEndretUtbetalingAndelerForPerson(
        andelerAvTypeForPerson: List<AndelTilkjentYtelse>,
        endretUtbetalingAndelerForPerson: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndelerForPerson.isEmpty()) {
            return andelerAvTypeForPerson
                .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it.copy()) }
        }

        val andelerTidslinje = andelerAvTypeForPerson.map { it.tilPeriode() }.tilTidslinje()
        val endretUtbetalingTidslinje = endretUtbetalingAndelerForPerson.tilTidslinje()

        val andelerMedEndringerTidslinje =
            andelerTidslinje.kombinerMed(endretUtbetalingTidslinje) { andelTilkjentYtelse, endretUtbetalingAndel ->
                if (andelTilkjentYtelse == null) {
                    null
                } else {
                    val endretUtbetalingErAlleredeUtbetaltSomFortsattSkalUtbetales =
                        endretUtbetalingAndel?.årsak == Årsak.ALLEREDE_UTBETALT && endretUtbetalingAndel.prosent!! > BigDecimal.ZERO

                    val andelSkalOverstyresAvEndretUtbetaling = endretUtbetalingAndel != null && !endretUtbetalingErAlleredeUtbetaltSomFortsattSkalUtbetales

                    val nyttBeløp =
                        if (andelSkalOverstyresAvEndretUtbetaling) {
                            andelTilkjentYtelse.sats.avrundetHeltallAvProsent(
                                endretUtbetalingAndel!!.prosent!!,
                            )
                        } else {
                            andelTilkjentYtelse.kalkulertUtbetalingsbeløp
                        }
                    val prosent =
                        if (andelSkalOverstyresAvEndretUtbetaling) endretUtbetalingAndel!!.prosent!! else andelTilkjentYtelse.prosent

                    AndelMedEndretUtbetalingForTidslinje(
                        aktør = andelTilkjentYtelse.aktør,
                        beløp = nyttBeløp,
                        sats = andelTilkjentYtelse.sats,
                        ytelseType = andelTilkjentYtelse.type,
                        prosent = prosent,
                        endretUtbetalingAndel = endretUtbetalingAndel,
                    )
                }
            }

        return andelerMedEndringerTidslinje
            .slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel()
            .tilAndelerTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse)
    }

    internal data class AndelMedEndretUtbetalingForTidslinje(
        val aktør: Aktør,
        val beløp: Int,
        val sats: Int,
        val ytelseType: YtelseType,
        val prosent: BigDecimal,
        val endretUtbetalingAndel: EndretUtbetalingAndelMedAndelerTilkjentYtelse?,
    )

    internal fun Tidslinje<AndelMedEndretUtbetalingForTidslinje>.tilAndelerTilkjentYtelseMedEndreteUtbetalinger(
        tilkjentYtelse: TilkjentYtelse,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> =
        this
            .tilPerioderIkkeNull()
            .map { it.tilAndelTilkjentYtelseMedEndreteUtbetalinger(tilkjentYtelse) }

    internal fun Periode<AndelMedEndretUtbetalingForTidslinje>.tilAndelTilkjentYtelseMedEndreteUtbetalinger(
        tilkjentYtelse: TilkjentYtelse,
    ): AndelTilkjentYtelseMedEndreteUtbetalinger {
        val andelTilkjentYtelse =
            AndelTilkjentYtelse(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = this.verdi.aktør,
                type = this.verdi.ytelseType,
                kalkulertUtbetalingsbeløp = this.verdi.beløp,
                nasjonaltPeriodebeløp = this.verdi.beløp,
                differanseberegnetPeriodebeløp = null,
                sats = this.verdi.sats,
                prosent = this.verdi.prosent,
                stønadFom = this.fom?.toYearMonth() ?: throw Feil("Fra og med-dato ikke satt"),
                stønadTom = this.tom?.toYearMonth() ?: throw Feil("Til og med-dato ikke satt"),
            )

        val endretUtbetalingAndel = this.verdi.endretUtbetalingAndel

        return if (endretUtbetalingAndel == null) {
            AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelse)
        } else {
            andelTilkjentYtelse.medEndring(endretUtbetalingAndel)
        }
    }

    internal fun Tidslinje<AndelMedEndretUtbetalingForTidslinje>.slåSammenEtterfølgende0krAndelerPgaSammeEndretAndel() =
        this
            .zipMedNeste(ZipPadding.FØR)
            .mapVerdi {
                val forrigeAndelMedEndring = it?.first
                val nåværendeAndelMedEndring = it?.second

                val forrigeOgNåværendeAndelEr0kr =
                    forrigeAndelMedEndring?.prosent == BigDecimal.ZERO && nåværendeAndelMedEndring?.prosent == BigDecimal.ZERO
                val forrigeOgNåværendeAndelErPåvirketAvSammeEndring =
                    forrigeAndelMedEndring?.endretUtbetalingAndel?.endretUtbetalingAndel == nåværendeAndelMedEndring?.endretUtbetalingAndel?.endretUtbetalingAndel &&
                        nåværendeAndelMedEndring?.endretUtbetalingAndel != null

                if (forrigeOgNåværendeAndelEr0kr && forrigeOgNåværendeAndelErPåvirketAvSammeEndring) forrigeAndelMedEndring else nåværendeAndelMedEndring
            }.filtrerIkkeNull()
}
