package no.nav.familie.ks.sak.common

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TestClockProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ClockProvider {
    override fun get(): Clock = clock

    companion object {
        private val zoneId = ZoneId.of("Europe/Oslo")

        fun lagClockProviderMedFastTidspunkt(zonedDateTime: ZonedDateTime): TestClockProvider = TestClockProvider(Clock.fixed(zonedDateTime.toInstant(), zoneId))

        fun lagClockProviderMedFastTidspunkt(localDateTime: LocalDateTime): TestClockProvider = TestClockProvider(Clock.fixed(localDateTime.atZone(zoneId).toInstant(), zoneId))

        fun lagClockProviderMedFastTidspunkt(localDate: LocalDate): TestClockProvider = lagClockProviderMedFastTidspunkt(localDate.atStartOfDay(zoneId))
    }
}
