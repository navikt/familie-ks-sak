package no.nav.familie.ks.sak.integrasjon.økonomi.internkonsistensavstemming

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal

fun erForskjellMellomAndelerOgOppdrag(
    andeler: List<AndelTilkjentYtelse>,
    utbetalingsoppdrag: Utbetalingsoppdrag?,
    fagsakId: Long,
): Boolean {
    val utbetalingsperioder =
        utbetalingsoppdrag
            ?.utbetalingsperiode
            ?.filter { it.opphør == null }
            ?: emptyList()

    val forskjellMellomAndeleneOgUtbetalingsoppdraget =
        hentForskjellIAndelerOgUtbetalingsoppdrag(utbetalingsperioder, andeler)

    when (forskjellMellomAndeleneOgUtbetalingsoppdraget) {
        is UtbetalingsperioderUtenTilsvarendeAndel -> {
            secureLogger.info(
                "Fagsak $fagsakId har sendt utbetalingsperiode(r) til økonomi som ikke har tilsvarende andel tilkjent ytelse." +
                    "\nDet er differanse i perioden(e) ${forskjellMellomAndeleneOgUtbetalingsoppdraget.utbetalingsperioder.tilTidStrenger()}." +
                    "\n\nSiste utbetalingsoppdrag som er sendt til familie-øknonomi på fagsaken er:" +
                    "\n$utbetalingsoppdrag",
            )
        }

        is IngenForskjell -> {
            Unit
        }
    }

    return forskjellMellomAndeleneOgUtbetalingsoppdraget !is IngenForskjell
}

private fun hentForskjellIAndelerOgUtbetalingsoppdrag(
    utbetalingsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>,
): AndelOgOppdragForskjell {
    val utbetalingsperioderUtenTilsvarendeAndel =
        utbetalingsperioder.filter {
            it.erIngenPersonerMedTilsvarendeAndelITidsrommet(andeler)
        }

    return if (utbetalingsperioderUtenTilsvarendeAndel.isEmpty()) {
        IngenForskjell
    } else {
        UtbetalingsperioderUtenTilsvarendeAndel(utbetalingsperioderUtenTilsvarendeAndel)
    }
}

private fun Utbetalingsperiode.erIngenPersonerMedTilsvarendeAndelITidsrommet(andeler: List<AndelTilkjentYtelse>): Boolean {
    val andelsTidslinjerPerPersonOgYtelsetype =
        andeler
            .groupBy { Pair(it.aktør, it.type) }
            .map { (_, andeler) -> andeler.tilBeløpstidslinje() }

    return andelsTidslinjerPerPersonOgYtelsetype.none {
        this.harTilsvarendeAndelerForPersonOgYtelsetype(it)
    }
}

private fun Utbetalingsperiode.harTilsvarendeAndelerForPersonOgYtelsetype(
    andelerTidslinjeForEnPersonOgYtelsetype: Tidslinje<BigDecimal>,
): Boolean {
    val erAndelLikUtbetalingTidslinje =
        this
            .tilBeløpstidslinje()
            .kombinerMed(andelerTidslinjeForEnPersonOgYtelsetype) { utbetalingsperiode, andel ->
                utbetalingsperiode?.let { utbetalingsperiode == andel }
            }

    return erAndelLikUtbetalingTidslinje.tilPerioder().all { it.verdi != false }
}

private fun Utbetalingsperiode.tilBeløpstidslinje() =
    listOf(
        Periode(
            fom = this.vedtakdatoFom.førsteDagIInneværendeMåned(),
            tom = this.vedtakdatoTom.sisteDagIMåned(),
            verdi = this.sats,
        ),
    ).tilTidslinje()

private fun List<AndelTilkjentYtelse>.tilBeløpstidslinje() =
    this
        .map {
            Periode(
                fom = it.stønadFom.atDay(1),
                tom = it.stønadTom.atEndOfMonth(),
                verdi = it.kalkulertUtbetalingsbeløp.toBigDecimal(),
            )
        }.tilTidslinje()

private fun List<Utbetalingsperiode>.tilTidStrenger() = slåSammen(this.map { "${it.vedtakdatoFom.toYearMonth()} til ${it.vedtakdatoTom.toYearMonth()}" })

private sealed interface AndelOgOppdragForskjell

private data class UtbetalingsperioderUtenTilsvarendeAndel(
    val utbetalingsperioder: List<Utbetalingsperiode>,
) : AndelOgOppdragForskjell

private object IngenForskjell : AndelOgOppdragForskjell
