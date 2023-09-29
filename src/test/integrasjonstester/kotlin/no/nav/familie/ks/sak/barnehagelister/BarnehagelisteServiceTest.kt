package no.nav.familie.ks.sak.no.nav.familie.ks.sak.barnehagelister

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.api.dto.BarnehagebarnRequestParams
import no.nav.familie.ks.sak.barnehagelister.BarnehageListeService
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.integrasjon.infotrygd.InfotrygdReplikaClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class BarnehagelisteServiceTest(
    @Autowired private val barnehageListeService: BarnehageListeService,
) : OppslagSpringRunnerTest() {

    @MockkBean(relaxed = true)
    private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    var barnehagelisteXml =
        """
        <ns2:melding xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:seres="http://seres.no/xsd/forvaltningsdata" 
        xmlns:ns1="http://seres.no/xsd/NAV/Barnehagelister_M/2017" xmlns:ns2="http://seres.no/xsd/NAV/Barnehagelister_M/201806" 
        xmlns:my="http://schemas.microsoft.com/office/infopath/2003/myXSD/2017-02-17T12:45:52" xmlns:xd="http://schemas.microsoft.com/office/infopath/2003">
        <ns2:skjema>
        <ns2:listeopplysninger>
            <ns2:kommuneNavn>Oslo</ns2:kommuneNavn>
            <ns2:kommuneNr>0301</ns2:kommuneNr>
            <ns2:aarInnsending>2023</ns2:aarInnsending>
            <ns2:maanedInnsending>09</ns2:maanedInnsending>
            <ns2:produsertDatoTid>2023-09-08T14:01:24.933+02:00</ns2:produsertDatoTid>
        </ns2:listeopplysninger>
        <ns2:barnInfolinje>
            <ns2:endringstype>barnStartet</ns2:endringstype>
            <ns2:barn>
                <ns2:navn>navnesen navn</ns2:navn>
                <ns2:fodselsnummer>123456789</ns2:fodselsnummer>
                <ns2:adresse>
                    <ns2:gateOgNummer>gate og nummer</ns2:gateOgNummer>
                    <ns2:postnummer>0655</ns2:postnummer>
                    <ns2:poststed>OSLO</ns2:poststed>
                </ns2:adresse>
            </ns2:barn>
            <ns2:forelder>
                <ns2:navn>navnesen navn</ns2:navn>
                <ns2:fodselsnummer>123456789</ns2:fodselsnummer>
                <ns2:adresse>
                    <ns2:gateOgNummer>gate og nummer</ns2:gateOgNummer>
                    <ns2:postnummer>0655</ns2:postnummer>
                    <ns2:poststed>OSLO</ns2:poststed>
                </ns2:adresse>
            </ns2:forelder>
            <ns2:forelder>
                <ns2:navn>navnesen navn</ns2:navn>
                <ns2:fodselsnummer>123456789</ns2:fodselsnummer>
                <ns2:adresse>
                    <ns2:gateOgNummer>gate og nummer</ns2:gateOgNummer>
                    <ns2:postnummer>0655</ns2:postnummer>
                    <ns2:poststed>OSLO</ns2:poststed>
                </ns2:adresse>
            </ns2:forelder>
            <ns2:barnehage>
                <ns2:navn>navnesen navn</ns2:navn>
                <ns2:organisasjonsnummer>0123412523</ns2:organisasjonsnummer>
                <ns2:adresse>
                    <ns2:gateOgNummer>gate og nummer</ns2:gateOgNummer>
                    <ns2:postnummer>0655</ns2:postnummer>
                    <ns2:poststed>OSLO</ns2:poststed>
                </ns2:adresse>
            </ns2:barnehage>
            <ns2:avtaltOppholdstidTimer>47.5</ns2:avtaltOppholdstidTimer>
            <ns2:startdato>2023-09-06</ns2:startdato>
        </ns2:barnInfolinje>
        </ns2:skjema>
        </ns2:melding>
        """

    @Test
    fun `test lagring av entiteter fra XML melding`() {
        val barnehagelisteMottatt = BarnehagelisteMottatt(
            melding = barnehagelisteXml,
            meldingId = "testId",
            mottatTid = LocalDateTime.now(),
        )
        barnehageListeService.lagreBarnehagelisteMottattOgOpprettTaskForLesing(barnehagelisteMottatt)
        assertNotNull(barnehageListeService.hentUarkiverteBarnehagelisteUuider())
        barnehageListeService.lesOgArkiver(barnehagelisteMottatt.id)
        val barnehagebarn = barnehageListeService.hentBarnehagebarn("123456789")
        assertNotNull(barnehagebarn)
        assertEquals("Oslo", barnehagebarn.kommuneNavn)
        assertEquals("0301", barnehagebarn.kommuneNr)
        assertEquals("123456789", barnehagebarn.ident)
        assertEquals(47.5, barnehagebarn.antallTimerIBarnehage)
        assertEquals("barnStartet", barnehagebarn.endringstype)
        assertEquals(LocalDate.parse("2023-09-06"), barnehagebarn.fom)
        assertNull(barnehagebarn.tom)
    }

    @Test
    fun `test at henting av data kombinert med data fra infotrygd fungerer`() {
        every { infotrygdReplikaClient.hentAlleBarnasIdenterForLøpendeFagsaker() } returns listOf("123456789")

        val barnehagelisteMottatt = BarnehagelisteMottatt(
            melding = barnehagelisteXml,
            meldingId = "testId",
            mottatTid = LocalDateTime.now(),
        )
        barnehageListeService.lagreBarnehagelisteMottattOgOpprettTaskForLesing(barnehagelisteMottatt)
        assertNotNull(barnehageListeService.hentUarkiverteBarnehagelisteUuider())
        barnehageListeService.lesOgArkiver(barnehagelisteMottatt.id)

        val barnehagebarn = barnehageListeService.hentAlleBarnehagebarnInfotrygd(
            BarnehagebarnRequestParams(
                kommuneNavn = "Oslo",
                kunLøpendeFagsak = false,
                ident = null,
            )
        )
        assertTrue(barnehagebarn.totalElements == 1L)
    }

    @Test
    fun `test parsing av barnehageliste melding XML til DTO`() {
        val melding = barnehageListeService.lesBarnehagelisteMottattMeldingXml(barnehagelisteXml)
        assertNotNull(melding)
        assertEquals("Oslo", melding.skjema.listeopplysninger.kommuneNavn)
        assertEquals("0301", melding.skjema.listeopplysninger.kommuneNr)
        assertEquals(1, melding.skjema.barnInfolinjer.size)
        assertEquals("123456789", melding.skjema.barnInfolinjer.get(0).barn.fodselsnummer)
        assertEquals(47.5, melding.skjema.barnInfolinjer.get(0).avtaltOppholdstidTimer)
        assertEquals("barnStartet", melding.skjema.barnInfolinjer.get(0).endringstype)
        assertEquals(LocalDate.parse("2023-09-06"), melding.skjema.barnInfolinjer.get(0).startdato)
        assertNull(melding.skjema.barnInfolinjer.get(0).sluttdato)
    }
}
