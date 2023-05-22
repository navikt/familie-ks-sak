package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammen
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.time.LocalDate

typealias Beløpsdifferanse = Int
typealias AktørId = String

enum class BehandlingAlder { NY, GAMMEL }

private data class AndelTilkjentYtelseDataForÅKalkulereEndring(
    val aktørId: AktørId,
    val kalkulertBeløp: Int,
    val endretUtbetalingÅrsaker: List<Årsak>,
    val behandlingAlder: BehandlingAlder
)

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.hentFørsteEndringstidspunkt(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): LocalDate? = this.hentPerioderMedEndringerFra(forrigeAndelerTilkjentYtelse)
    .mapNotNull { (_, tidslinjeMedDifferanserPåPerson) ->
        tidslinjeMedDifferanserPåPerson.tilPerioder().minOfOrNull { checkNotNull(it.fom) }
    }.minOfOrNull { it }

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.hentPerioderMedEndringerFra(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): Map<AktørId, Tidslinje<Beløpsdifferanse>> {
    val andelerTidslinje = this.hentTidslinjerForPersoner(BehandlingAlder.NY)
    val forrigeAndelerTidslinje = forrigeAndelerTilkjentYtelse.hentTidslinjerForPersoner(BehandlingAlder.GAMMEL)

    val personerFraForrigeEllerDenneBehandlinger =
        (this.map { it.aktør.aktørId } + forrigeAndelerTilkjentYtelse.map { it.aktør.aktørId }).toSet()

    return personerFraForrigeEllerDenneBehandlinger.associateWith { aktørId ->
        val tidslinjeForPerson = andelerTidslinje[aktørId]
            ?: Tidslinje(startsTidspunkt = TIDENES_MORGEN.plusDays(1), perioder = emptyList())
        val forrigeTidslinjeForPerson = forrigeAndelerTidslinje[aktørId]
            ?: Tidslinje(startsTidspunkt = TIDENES_MORGEN.plusDays(1), perioder = emptyList())
        val kombinertTidslinje = listOf(tidslinjeForPerson, forrigeTidslinjeForPerson).slåSammen()

        kombinertTidslinje.tilPerioderIkkeNull().mapNotNull { it.tilPeriodeMedEndringer() }.tilTidslinje()
    }.filter { it.value.tilPerioder().isNotEmpty() }
}

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.hentTidslinjerForPersoner(
    behandlingAlder: BehandlingAlder
): Map<String, Tidslinje<AndelTilkjentYtelseDataForÅKalkulereEndring>> =
    this.groupBy { it.aktør.aktørId }.map { (aktørId, andeler) ->
        aktørId to andeler.hentTidslinje(behandlingAlder)
    }.toMap()

private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.hentTidslinje(
    behandlingAlder: BehandlingAlder
): Tidslinje<AndelTilkjentYtelseDataForÅKalkulereEndring> =
    this.map {
        Periode(
            fom = it.stønadFom.førsteDagIInneværendeMåned(),
            tom = it.stønadTom.sisteDagIInneværendeMåned(),
            verdi = AndelTilkjentYtelseDataForÅKalkulereEndring(
                aktørId = it.aktør.aktørId,
                kalkulertBeløp = it.kalkulertUtbetalingsbeløp,
                endretUtbetalingÅrsaker = it.endreteUtbetalinger.mapNotNull { endretUtbetalingAndel -> endretUtbetalingAndel.årsak },
                behandlingAlder = behandlingAlder
            )
        )
    }.tilTidslinje()

private fun Periode<Collection<AndelTilkjentYtelseDataForÅKalkulereEndring>>.tilPeriodeMedEndringer(): Periode<Int>? {
    val erEndring = erEndringPåPersonISegment(this.verdi)

    return if (erEndring) {
        Periode(fom = this.fom, tom = this.tom, verdi = hentBeløpsendringPåPersonIPeriode(this.verdi))
    } else {
        null
    }
}

private fun erEndringPåPersonISegment(nyOgGammelDataPåBrukerIPerioden: Collection<AndelTilkjentYtelseDataForÅKalkulereEndring>): Boolean {
    val nyttBeløp = nyOgGammelDataPåBrukerIPerioden.finnKalkulertBeløp(BehandlingAlder.NY)
    val gammeltBeløp = nyOgGammelDataPåBrukerIPerioden.finnKalkulertBeløp(BehandlingAlder.GAMMEL)

    val nyEndretUtbetalingÅrsaker =
        nyOgGammelDataPåBrukerIPerioden.find { it.behandlingAlder == BehandlingAlder.NY }?.endretUtbetalingÅrsaker?.sorted()
    val gammelEndretUtbetalingÅrsaker =
        nyOgGammelDataPåBrukerIPerioden.find { it.behandlingAlder == BehandlingAlder.GAMMEL }?.endretUtbetalingÅrsaker?.sorted()

    return nyttBeløp != gammeltBeløp || nyEndretUtbetalingÅrsaker != gammelEndretUtbetalingÅrsaker
}

private fun hentBeløpsendringPåPersonIPeriode(nyOgGammelDataPåBrukerISegmentet: Collection<AndelTilkjentYtelseDataForÅKalkulereEndring>): Int {
    val nyttBeløp = nyOgGammelDataPåBrukerISegmentet.finnKalkulertBeløp(BehandlingAlder.NY) ?: 0
    val gammeltBeløp = nyOgGammelDataPåBrukerISegmentet.finnKalkulertBeløp(BehandlingAlder.GAMMEL) ?: 0

    return nyttBeløp - gammeltBeløp
}

private fun Collection<AndelTilkjentYtelseDataForÅKalkulereEndring>.finnKalkulertBeløp(behandlingAlder: BehandlingAlder) =
    singleOrNull { it.behandlingAlder == behandlingAlder }?.kalkulertBeløp
