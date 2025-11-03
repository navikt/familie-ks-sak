package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattArkiv
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattArkivRepository
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattRepository
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BarnehagelisteServiceTest {
    private val mockBarnehagelisteMottattRepository = mockk<BarnehagelisteMottattRepository>()
    private val mockBarnehagebarnRepository = mockk<BarnehagebarnRepository>()
    private val mockTaskService = mockk<TaskRepositoryWrapper>()
    private val mockBarnehagelisteMottattArkivRepository = mockk<BarnehagelisteMottattArkivRepository>()

    private val barnehageListeService =
        BarnehageListeService(
            mockBarnehagelisteMottattRepository,
            mockBarnehagebarnRepository,
            mockTaskService,
            mockBarnehagelisteMottattArkivRepository,
        )

    @Test
    fun `lagreBarnehagelisteMottattOgOpprettTaskForLesing skal lagre barnehageliste og opprette task for lesing`() {
        // Arrange
        val barnehagelisteMottatt =
            BarnehagelisteMottatt(
                meldingId = "1234",
                melding = barnehagelisteXml,
                mottatTid = LocalDateTime.now(),
            )

        val taskSlot = slot<Task>()

        every { mockBarnehagelisteMottattRepository.save(barnehagelisteMottatt) } returns barnehagelisteMottatt
        every { mockTaskService.save(capture(taskSlot)) } returnsArgument 0

        // Act
        val lagretBarnehagelisteMottatt =
            barnehageListeService.lagreBarnehagelisteMottattOgOpprettTaskForLesing(barnehagelisteMottatt)

        // Assert
        assertThat(lagretBarnehagelisteMottatt).isEqualTo(barnehagelisteMottatt)

        val opprettetTask = taskSlot.captured

        assertThat(opprettetTask.payload).isEqualTo(lagretBarnehagelisteMottatt.id.toString())
        assertThat(opprettetTask.metadata["AR-referanse"]).isEqualTo(lagretBarnehagelisteMottatt.meldingId)

        verify(exactly = 1) { mockTaskService.save(opprettetTask) }
        verify(exactly = 1) { mockBarnehagelisteMottattRepository.save(barnehagelisteMottatt) }
    }

    @Test
    fun `erListenMottattTidligere skal returnere true dersom liste er mottatt og tidligere lagret i repository`() {
        // Arrange
        every { mockBarnehagelisteMottattRepository.existsByMeldingId("finnes") } returns true

        // Act
        val erListenMottattTidligere = barnehageListeService.erBarnehagelisteMottattTidligere("finnes")

        // Assert
        assertThat(erListenMottattTidligere).isTrue()

        verify(exactly = 1) { mockBarnehagelisteMottattRepository.existsByMeldingId("finnes") }
        verify(exactly = 0) { mockBarnehagelisteMottattArkivRepository.existsByMeldingId("finnes") }
    }

    @Test
    fun `erListenMottattTidligere skal returnere true dersom liste er mottatt og tidligere lagret i arkiv repository`() {
        // Arrange
        every { mockBarnehagelisteMottattRepository.existsByMeldingId("finnes") } returns false
        every { mockBarnehagelisteMottattArkivRepository.existsByMeldingId("finnes") } returns true

        // Act
        val erListenMottattTidligere = barnehageListeService.erBarnehagelisteMottattTidligere("finnes")

        // Assert
        assertThat(erListenMottattTidligere).isTrue()

        verify(exactly = 1) { mockBarnehagelisteMottattRepository.existsByMeldingId("finnes") }
        verify(exactly = 1) { mockBarnehagelisteMottattArkivRepository.existsByMeldingId("finnes") }
    }

    @Test
    fun `erListenMottattTidligere skal returnere false dersom liste ikke er tidligere mottatt i repository og arkiv repository`() {
        // Arrange
        every { mockBarnehagelisteMottattRepository.existsByMeldingId("finnes ikke") } returns false
        every { mockBarnehagelisteMottattArkivRepository.existsByMeldingId("finnes ikke") } returns false

        // Act
        val erListenMottattTidligere = barnehageListeService.erBarnehagelisteMottattTidligere("finnes ikke")

        // Assert
        assertThat(erListenMottattTidligere).isFalse()

        verify(exactly = 1) { mockBarnehagelisteMottattRepository.existsByMeldingId("finnes ikke") }
        verify(exactly = 1) { mockBarnehagelisteMottattArkivRepository.existsByMeldingId("finnes ikke") }
    }

    @Test
    fun `lesOgArkiver skal bare arkivere dersom UUID eksisterer`() {
        // Arrange
        val ikkeEksisterendeUUID = UUID.randomUUID()
        every { mockBarnehagelisteMottattRepository.findByIdOrNull(ikkeEksisterendeUUID) } returns null

        // Act
        barnehageListeService.lesOgArkiverBarnehageliste(ikkeEksisterendeUUID)

        // Assert
        verify(exactly = 1) { mockBarnehagelisteMottattRepository.findByIdOrNull(ikkeEksisterendeUUID) }
        verify(exactly = 0) { mockBarnehagelisteMottattRepository.save(any()) }
        verify(exactly = 0) { mockBarnehagelisteMottattRepository.deleteById(any()) }
    }

    @Test
    fun `lesOgArkiver lagre barnehagelister, lagre liste i arkiv og slette det fra mottatt repository`() {
        // Arrange
        val eksisterendeUUID = UUID.randomUUID()
        val barnehagelisteMottatt =
            BarnehagelisteMottatt(
                meldingId = "1234",
                melding = barnehagelisteXml,
                mottatTid = LocalDateTime.now(),
            )
        val barnehagelisteMottattArkivSlot = slot<BarnehagelisteMottattArkiv>()
        val barnehagebarnListSlot = slot<List<Barnehagebarn>>()

        every { mockBarnehagelisteMottattRepository.findByIdOrNull(eksisterendeUUID) } returns barnehagelisteMottatt
        every { mockBarnehagelisteMottattArkivRepository.save(capture(barnehagelisteMottattArkivSlot)) } returns mockk()
        every { mockBarnehagebarnRepository.saveAll(capture(barnehagebarnListSlot)) } returns mockk()
        every { mockBarnehagelisteMottattRepository.deleteById(barnehagelisteMottatt.id) } just runs

        // Act
        barnehageListeService.lesOgArkiverBarnehageliste(eksisterendeUUID)

        // Assert
        val capturedBarnehagelisteMottattArkiv = barnehagelisteMottattArkivSlot.captured

        assertThat(capturedBarnehagelisteMottattArkiv.id).isEqualTo(barnehagelisteMottatt.id)
        assertThat(capturedBarnehagelisteMottattArkiv.melding).isEqualTo(barnehagelisteMottatt.melding)
        assertThat(capturedBarnehagelisteMottattArkiv.mottatTid).isEqualTo(barnehagelisteMottatt.mottatTid)
        assertThat(capturedBarnehagelisteMottattArkiv.meldingId).isEqualTo(barnehagelisteMottatt.meldingId)

        val capturedBarnehagebarnList = barnehagebarnListSlot.captured

        val barnMedIdent123456789 = capturedBarnehagebarnList.single { it.ident == "123456789" }

        assertThat(barnMedIdent123456789.ident).isEqualTo("123456789")
        assertThat(barnMedIdent123456789.fom).isEqualTo(LocalDate.of(2023, 9, 6))
        assertThat(barnMedIdent123456789.tom).isNull()
        assertThat(barnMedIdent123456789.antallTimerIBarnehage).isEqualTo(47.5)
        assertThat(barnMedIdent123456789.endringstype).isEqualTo("barnStartet")
        assertThat(barnMedIdent123456789.kommuneNavn).isEqualTo("Oslo")
        assertThat(barnMedIdent123456789.kommuneNr).isEqualTo("0301")
        assertThat(barnMedIdent123456789.arkivReferanse).isEqualTo("1234")

        verify(exactly = 1) { mockBarnehagelisteMottattRepository.findByIdOrNull(eksisterendeUUID) }
        verify(exactly = 1) { mockBarnehagelisteMottattArkivRepository.save(capturedBarnehagelisteMottattArkiv) }
        verify(exactly = 1) { mockBarnehagebarnRepository.saveAll(any<List<Barnehagebarn>>()) }
        verify(exactly = 1) { mockBarnehagelisteMottattRepository.deleteById(barnehagelisteMottatt.id) }
    }

    companion object {
        var barnehagelisteXml = """
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
        <ns2:barnInfolinje>
            <ns2:endringstype>barnStartet</ns2:endringstype>
            <ns2:barn>
                <ns2:navn>navnesen navn</ns2:navn>
                <ns2:fodselsnummer>123456780</ns2:fodselsnummer>
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
    }
}
