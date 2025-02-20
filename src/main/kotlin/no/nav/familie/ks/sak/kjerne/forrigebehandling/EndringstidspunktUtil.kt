package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder

internal fun Tidslinje<Boolean>.tilFørsteEndringstidspunkt() =
    this
        .tilPerioder()
        .filter { it.verdi == true }
        .mapNotNull { it.fom }
        .minOfOrNull { it }
        ?.toYearMonth()
