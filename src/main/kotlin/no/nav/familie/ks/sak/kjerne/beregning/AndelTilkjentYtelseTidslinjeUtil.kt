package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.util.MAX_MÅNED
import no.nav.familie.ks.sak.common.util.MIN_MÅNED
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

fun Iterable<AndelTilkjentYtelse>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse>> =
    this
        .groupBy { it.aktør }
        .mapValues { (_, andeler) -> andeler.map { it.tilPeriode() }.tilTidslinje() }

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> = this.values.flatMap { it.tilAndelerTilkjentYtelse() }

fun Tidslinje<AndelTilkjentYtelse>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> =
    this
        .tilPerioder()
        .map {
            it.verdi?.medPeriode(
                it.fom?.tilYearMonth(),
                it.tom?.tilYearMonth(),
            )
        }.filterNotNull()

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
) = copy(
    id = 0,
    stønadFom = fraOgMed ?: MIN_MÅNED,
    stønadTom = tilOgMed ?: MAX_MÅNED,
).also { versjon = this.versjon }
