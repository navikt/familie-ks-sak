package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnehagelisteMottattRepositoryTest(
    @Autowired private val barnehagelisteMottattRepository: BarnehagelisteMottattRepository,
) : OppslagSpringRunnerTest() {

    @Test
    fun `test lagring av mottatt barnehageliste og existsBymeldingId`() {
        barnehagelisteMottattRepository.save(
            BarnehagelisteMottatt(
                meldingId = "1234",
                melding = """
                   <?xml version="1.0" encoding="utf-8"?>
                    <melding>
                       <skjema>
                         <listeopplysninger>
                           <kommuneNavn>Bærum kommune</kommuneNavn>
                           <kommuneNr>0219</kommuneNr>
                           <aarInnsending>2017</aarInnsending>
                           <maanedInnsending>01</maanedInnsending>
                           <produsertDatoTid>2017-01-13T12:12:12</produsertDatoTid>
                         </listeopplysninger>
                         <barnInfolinjer>
                           <barnInfolinje>
                             <endringstype>barnStartet</endringstype>
                             <barn>
                               <navn>Ekte Eføy</navn>
                               <fodselsnummer>14127011819</fodselsnummer>
                               <adresse>
                                 <gateOgNummer>Ringstabekkveien 1</gateOgNummer>
                                 <postnummer>1300</postnummer>
                                 <poststed>Ringstabekk</poststed>
                               </adresse>
                             </barn>
                             <foreldre>
                               <forelder>
                                 <navn>Ustabil Bensin</navn>
                                 <fodselsnummer>17058206660</fodselsnummer>
                                 <adresse>
                                    <gateOgNummer>Ringstabekkveien 1</gateOgNummer>
                                    <postnummer>1300</postnummer>
                                    <poststed>Ringstabekk</poststed>
                                 </adresse>
                               </forelder>
                             </foreldre>
                             <barnehage>
                               <navn>Tertitten barnehage</navn>
                               <organisasjonsnummer>970012321</organisasjonsnummer>
                               <adresse>
                                 <gateOgNummer>Sandvika 2</gateOgNummer>
                                 <postnummer>1200</postnummer>
                                 <poststed>Sandvika</poststed>
                               </adresse>
                             </barnehage>
                             <avtaltOppholdstidTimer>47.5</avtaltOppholdstidTimer>
                             <endretOppholdstidDato>2016-01-10</endretOppholdstidDato>
                             <startdato>2016-01-10</startdato>
                             <sluttdato>2018-08-01</sluttdato>
                           </barnInfolinje>
                         </barnInfolinjer>
                       </skjema>
                    </melding>
                """.trimIndent(),
                mottatTid = LocalDateTime.now(),
            ),
        )

        assertTrue(barnehagelisteMottattRepository.existsByMeldingId("1234"))

        val barnehagelisterMottatt = barnehagelisteMottattRepository.findAll()
        assertNotNull(barnehagelisterMottatt)
    }
}
