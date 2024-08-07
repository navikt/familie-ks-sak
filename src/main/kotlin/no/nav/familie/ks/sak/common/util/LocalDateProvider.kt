package no.nav.familie.ks.sak.common.util

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

interface LocalDateProvider {
    fun now(): LocalDate
}

@Service
class RealDateProvider : LocalDateProvider {
    override fun now() = LocalDate.now()
}

interface LocalDateTimeProvider {
    fun now(): LocalDateTime
}

@Service
class RealDateTimerProvider : LocalDateTimeProvider {
    override fun now() = LocalDateTime.now()
}
