package no.nav.familie.ks.sak.common.util

import org.springframework.stereotype.Service
import java.time.LocalDate

interface LocalDateProvider {
    fun now(): LocalDate
}

@Service
class RealDateProvider : LocalDateProvider {
    override fun now() = LocalDate.now()
}
