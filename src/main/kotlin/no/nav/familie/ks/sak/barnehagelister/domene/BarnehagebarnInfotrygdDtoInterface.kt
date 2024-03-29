package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate
import java.time.LocalDateTime

interface BarnehagebarnInfotrygdDtoInterface {
    fun getIdent(): String

    fun getFom(): LocalDate

    fun getTom(): LocalDate?

    fun getAntallTimerIBarnehage(): Double

    fun getEndringstype(): String

    fun getKommuneNavn(): String

    fun getKommuneNr(): String

    fun getEndretTid(): LocalDateTime
}
