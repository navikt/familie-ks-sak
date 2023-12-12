package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.MAX_MÅNED
import no.nav.familie.ks.sak.common.util.MIN_MÅNED
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.YearMonth

fun Iterable<AndelTilkjentYtelse>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse>> {
    return this.groupBy { it.aktør }
        .mapValues { (_, andeler) -> andeler.map { it.tilPeriode() }.tilTidslinje() }
}

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.values.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Iterable<Tidslinje<AndelTilkjentYtelse>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Tidslinje<AndelTilkjentYtelse>.tilAndelTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this
        .tilPerioder().map {
            it.verdi?.medPeriode(
                it.fom?.tilYearMonth(),
                it.tom?.tilYearMonth(),
            )
        }.filterNotNull()
}

fun AndelTilkjentYtelse.tilPeriode() =
    Periode(
        // Ta bort periode, slik at det ikke blir med på innholdet som vurderes for likhet
        verdi = this.medPeriode(null, null),
        fom = this.stønadFom.førsteDagIInneværendeMåned(),
        tom = this.stønadTom.sisteDagIInneværendeMåned(),
    )

fun AndelTilkjentYtelse.medPeriode(
    fraOgMed: YearMonth?,
    tilOgMed: YearMonth?,
) =
    copy(
        id = 0,
        stønadFom = fraOgMed ?: MIN_MÅNED,
        stønadTom = tilOgMed ?: MAX_MÅNED,
    ).also { versjon = this.versjon }

data class AndelTilkjentYtelseForTidslinje(
    val aktør: Aktør,
    val beløp: Int,
    val sats: Int,
    val ytelseType: YtelseType,
    val prosent: BigDecimal,
    val nasjonaltPeriodebeløp: Int = beløp,
    val differanseberegnetPeriodebeløp: Int? = null,
)

fun AndelTilkjentYtelse.tilpassTilTidslinje() =
    AndelTilkjentYtelseForTidslinje(
        aktør = this.aktør,
        beløp = this.kalkulertUtbetalingsbeløp,
        ytelseType = this.type,
        sats = this.sats,
        prosent = this.prosent,
        nasjonaltPeriodebeløp = this.nasjonaltPeriodebeløp ?: this.kalkulertUtbetalingsbeløp,
        differanseberegnetPeriodebeløp = this.differanseberegnetPeriodebeløp,
    )

fun Tidslinje<AndelTilkjentYtelseForTidslinje>.tilAndelerTilkjentYtelse(tilkjentYtelse: TilkjentYtelse): List<AndelTilkjentYtelse> {
    return this.tilPerioder()
        .filter { it.verdi != null }
        .map {
            AndelTilkjentYtelse(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = it.verdi!!.aktør,
                type = it.verdi.ytelseType,
                kalkulertUtbetalingsbeløp = it.verdi.beløp,
                nasjonaltPeriodebeløp = it.verdi.nasjonaltPeriodebeløp,
                differanseberegnetPeriodebeløp = it.verdi.differanseberegnetPeriodebeløp,
                sats = it.verdi.sats,
                prosent = it.verdi.prosent,
                stønadFom = it.fom?.tilYearMonth() ?: error("Fom på andel tilkjent ytelse skal ikke være null"),
                stønadTom = it.tom?.tilYearMonth() ?: error("Tom på andel tilkjent ytelse skal ikke være null"),
            )
        }
}
