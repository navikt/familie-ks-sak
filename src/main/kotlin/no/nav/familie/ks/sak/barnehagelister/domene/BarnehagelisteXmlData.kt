package no.nav.familie.ks.sak.barnehagelister.domene

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.time.LocalDate

@JacksonXmlRootElement(namespace = "ns2", localName = "melding")
data class Melding(
    var skjema: Skjema,
)

@JacksonXmlRootElement(namespace = "ns2", localName = "skjema")
data class Skjema(
    @JacksonXmlProperty(localName = "barnInfolinje")
    @JacksonXmlElementWrapper(useWrapping = false)
    var barnInfolinjer: List<BarnInfolinje>,

    @JacksonXmlProperty(localName = "listeopplysninger")
    var listeopplysninger: Listeopplysninger,
)

data class Listeopplysninger(
    var kommuneNavn: String,
    var kommuneNr: String,
)
data class BarnInfolinje(
    var avtaltOppholdstidTimer: Double,
    var startdato: LocalDate,
    var sluttdato: LocalDate?,
    var barn: Barn,
    var endringstype: String,
) {

    fun tilBarnehagelisteEntitet(kommuneNavn: String, kommuneNr: String): Barnehagebarn {
        return Barnehagebarn(
            ident = this.barn.fodselsnummer,
            fom = this.startdato,
            tom = this.sluttdato,
            antallTimerIBarnehage = this.avtaltOppholdstidTimer,
            endringstype = this.endringstype,
            kommuneNavn = kommuneNavn,
            kommuneNr = kommuneNr,
        )
    }
}
data class Barn(
    var fodselsnummer: String,
)
