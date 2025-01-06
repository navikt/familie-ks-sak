package no.nav.familie.ks.sak

import no.nav.familie.ks.sak.common.ClockProvider
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TestClockProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : ClockProvider {
    override fun get(): Clock = clock

    companion object {
        private val zoneId = ZoneId.of("Europe/Oslo")

        fun lagClockProviderMedFastTidspunkt(localDateTime: ZonedDateTime): TestClockProvider = TestClockProvider(Clock.fixed(localDateTime.toInstant(), zoneId))

        fun lagClockProviderMedFastTidspunkt(localDate: LocalDate): TestClockProvider = lagClockProviderMedFastTidspunkt(localDate.atStartOfDay(zoneId))
    }
}
