package no.nav.familie.ks.sak.barnehagelister.domene

import no.nav.familie.ks.sak.config.KafkaConfig
import java.time.LocalDate
import java.util.UUID

class KSBarnehagebarnDTO(
    val id: UUID,
    var ident: String,
    var fom: LocalDate,
    var tom: LocalDate? = null,
    var antallTimerIBarnehage: Double,
    var kommuneNavn: String,
    var kommuneNr: String,
    var barnehagelisteId: String,
) {
    fun tilBarnehagebarn(): Barnehagebarn =
        Barnehagebarn(
            id = id,
            ident = ident,
            fom = fom,
            tom = tom,
            antallTimerIBarnehage = antallTimerIBarnehage,
            kommuneNavn = kommuneNavn,
            kommuneNr = kommuneNr,
            arkivReferanse = barnehagelisteId,
            kildeTopic = KafkaConfig.BARNEHAGELISTE_TOPIC,
        )
}
