package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.LocalDate
import java.time.YearMonth

internal fun Tidslinje<Boolean>.tilFørsteEndringstidspunkt() =
    this
        .tilPerioder()
        .filter { it.verdi == true }
        .mapNotNull { it.fom }
        .minOfOrNull { it }
        ?.toYearMonth()

/**
 * Utleder første endringstidspunkt fra fire mulige datoer basert på første endring av:
 * - utbetaling
 * - kompetanse
 * - vilkårsvurdering
 * - endret utbetaling andel
 * Hvis det ikke er endring på feks. utbetaling blir den datoen null.
 * Hvis det ikke finnes noen endring i det hele tatt (dvs. alle er null) setter vi endringstidspunkt til tidenes ende
 * Dette er for at vi dermed kun skal få med vedtaksperioder som kun strekker seg uendelig frem i tid (feks. opphørsperiode)
 * * */
internal fun utledEndringstidspunkt(
    endringstidspunktUtbetalingsbeløp: YearMonth?,
    endringstidspunktKompetanse: YearMonth?,
    endringstidspunktVilkårsvurdering: YearMonth?,
    endringstidspunktEndretUtbetalingAndeler: YearMonth?,
): LocalDate =
    listOfNotNull(
        endringstidspunktUtbetalingsbeløp,
        endringstidspunktKompetanse,
        endringstidspunktVilkårsvurdering,
        endringstidspunktEndretUtbetalingAndeler,
    ).minOfOrNull { it }?.førsteDagIInneværendeMåned() ?: TIDENES_ENDE
