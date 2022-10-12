package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.tidslinje.PeriodeVerdi
import java.time.LocalDate

data class TidslinjePeriodeMedDatoLocalDate<T>(
    val periodeVerdi: PeriodeVerdi<T>,
    val fom: LocalDate,
    val tom: LocalDate,
    var erUendelig: Boolean = false
)
