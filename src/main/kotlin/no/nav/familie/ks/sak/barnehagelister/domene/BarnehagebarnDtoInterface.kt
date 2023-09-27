package no.nav.familie.ks.sak.barnehagelister.domene

import java.time.LocalDate

interface BarnehagebarnDtoInterface {

    fun getId(): String
    fun getIdent(): String
    fun getFom(): LocalDate
    fun getTom(): LocalDate
    fun getAntallTimerIBarnehage(): Double
    fun getEndringstype(): String
    fun getKommuneNavn(): String
    fun getKommuneNr(): String
    fun getBehandlingId(): Long
    fun getFagsakId(): Long
    fun getFagsakstatus(): String
}
