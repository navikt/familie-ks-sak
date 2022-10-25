package no.nav.familie.ks.sak.task

import no.nav.familie.ks.sak.common.util.erHverdag
import no.nav.familie.ks.sak.common.util.erKlokkenMellom21Og06
import no.nav.familie.ks.sak.common.util.kl06IdagEllerNesteDag
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Finner neste gyldige kjøringstidspunkt for tasker som kun skal kjøre på "dagtid".
 *
 * Dagtid er nå definert som hverdager mellom 06-21. Faste helligdager er tatt høyde for, men flytende
 * er ikke kodet inn.
 */
fun nesteGyldigeTriggertidForBehandlingIHverdager(
    minutesToAdd: Long = 0,
    triggerTid: LocalDateTime = LocalDateTime.now()
): LocalDateTime {
    var date = triggerTid.plusMinutes(minutesToAdd)

    date = if (erKlokkenMellom21Og06(date.toLocalTime()) && date.erHverdag(1)) {
        kl06IdagEllerNesteDag(date)
    } else if (erKlokkenMellom21Og06(date.toLocalTime()) || !date.erHverdag(0)) {
        date.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(6)
    } else {
        date
    }

    when {
        date.dayOfMonth == 1 && date.month == Month.JANUARY -> date = date.plusDays(1)
        date.dayOfMonth == 1 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 17 && date.month == Month.MAY -> date = date.plusDays(1)
        date.dayOfMonth == 25 && date.month == Month.DECEMBER -> date = date.plusDays(2)
        date.dayOfMonth == 26 && date.month == Month.DECEMBER -> date = date.plusDays(1)
    }

    return date
}
