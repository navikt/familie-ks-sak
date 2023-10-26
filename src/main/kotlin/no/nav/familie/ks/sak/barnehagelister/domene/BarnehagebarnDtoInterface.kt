package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate
import java.time.LocalDateTime

interface BarnehagebarnDtoInterface {
    fun getIdent(): String

    fun getFom(): LocalDate

    fun getTom(): LocalDate?

    fun getAntallTimerIBarnehage(): Double

    fun getEndringstype(): String

    fun getKommuneNavn(): String

    fun getKommuneNr(): String

    fun getFagsakId(): Long?

    fun getFagsakstatus(): String?

    fun getEndretTid(): LocalDateTime
}
