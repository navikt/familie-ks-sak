package no.nav.familie.ks.sak.common.tidslinje

import java.time.LocalDate

data class TidslinjePeriodeMedDatoLocalDate<T>(
    val periodeVerdi: PeriodeVerdi<T>,
    val fom: LocalDate,
    val tom: LocalDate,
    var erUendelig: Boolean = false
)
