package no.nav.familie.ks.sak.kjerne.personident

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomAktørId
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PersonidentServiceTest {
    private val personidentRepository = mockk<PersonidentRepository>()
    private val aktørRepository = mockk<AktørRepository>()
    private val pdlClient = mockk<PdlClient>()
    private val taskService = mockk<TaskRepositoryWrapper>()

    private val personidentService =
        PersonidentService(
            personidentRepository = personidentRepository,
            aktørRepository = aktørRepository,
            pdlClient = pdlClient,
            taskService = taskService,
        )

    @Test
    fun `hentOgLagreAktør - skal hente personident fra personidentRepository dersom personident finnes i db`() {
        val personnummer = randomFnr()
        val personIdent = Personident(personnummer, randomAktør(personnummer))
        every { personidentRepository.findByFødselsnummerOrNull(personnummer) } returns personIdent

        val hentetAktør = personidentService.hentOgLagreAktør(personnummer, true)
        assertNotNull(hentetAktør)
    }

    @Test
    fun `hentOgLagreAktør - skal hente aktør fra aktørRepository dersom aktørId finnes i db`() {
        val fødselsnummer = randomFnr()
        val aktør = randomAktør(fødselsnummer)
        every { personidentRepository.findByFødselsnummerOrNull(aktør.aktørId) } returns null
        every { aktørRepository.findByAktørId(aktør.aktørId) } returns aktør

        val hentetAktør = personidentService.hentOgLagreAktør(aktør.aktørId, true)
        assertNotNull(hentetAktør)
    }

    @Test
    fun `hentOgLagreAktør - skal hente personident fra personidentRepository dersom aktivt fødselsnummer fra PDL finnes i db`() {
        val fødselsnummer = randomFnr()

        val pdlFødselsnummer = randomFnr()
        val pdlIdent = PdlIdent(pdlFødselsnummer, false, "FOLKEREGISTERIDENT")
        val personident = Personident(pdlFødselsnummer, randomAktør(pdlFødselsnummer))

        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns null
        every { personidentRepository.findByFødselsnummerOrNull(pdlFødselsnummer) } returns personident
        every { aktørRepository.findByAktørId(fødselsnummer) } returns null
        every { pdlClient.hentIdenter(any(), false) } returns listOf(pdlIdent)

        val hentetAktør = personidentService.hentOgLagreAktør(fødselsnummer, true)

        assertEquals(personident.aktør, hentetAktør)
    }

    @Test
    fun `hentOgLagreAktør - skal hente aktør fra aktørRepository og opprette ny personident dersom aktiv aktørId fra PDL finnes i db`() {
        val fødselsnummer = randomFnr()

        val pdlFødselsnummer = randomFnr()
        val personIdentPDL = PdlIdent(pdlFødselsnummer, false, "FOLKEREGISTERIDENT")
        val pdlAktør = Aktør(randomAktørId())
        val pdlAktørMedPersonIdent = Aktør(pdlAktør.aktørId)
        pdlAktørMedPersonIdent.personidenter.add(Personident(pdlFødselsnummer, pdlAktørMedPersonIdent))
        val aktørIdentPDL = PdlIdent(pdlAktør.aktørId, false, "AKTORID")

        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns null
        every { personidentRepository.findByFødselsnummerOrNull(pdlFødselsnummer) } returns null
        every { aktørRepository.findByAktørId(fødselsnummer) } returns null
        every { aktørRepository.findByAktørId(pdlAktør.aktørId) } returns pdlAktør
        every { aktørRepository.saveAndFlush(pdlAktør) } returns pdlAktør
        every { aktørRepository.saveAndFlush(pdlAktørMedPersonIdent) } returns pdlAktørMedPersonIdent
        every { pdlClient.hentIdenter(any(), false) } returns listOf(personIdentPDL, aktørIdentPDL)

        val hentetAktør = personidentService.hentOgLagreAktør(fødselsnummer, true)

        // Validerer at aktør lagres før og etter at personIdent er lagt til. Noe greier med index issues.
        verify(exactly = 2) {
            aktørRepository.saveAndFlush(
                withArg { assertEquals(pdlAktør.aktørId, it.aktørId) },
            )
        }

        assertEquals(pdlAktørMedPersonIdent, hentetAktør)
    }

    @Test
    fun `hentOgLagreAktør - skal opprette aktør og personident med aktørId og personident fra PDL dersom verken aktørId eller fødselsnummer fra PDL finnes i db fra før`() {
        val fødselsnummer = randomFnr()

        val pdlFødselsnummer = randomFnr()
        val personIdentPDL = PdlIdent(pdlFødselsnummer, false, "FOLKEREGISTERIDENT")
        val pdlAktør = Aktør(randomAktørId())
        val aktørIdentPDL = PdlIdent(pdlAktør.aktørId, false, "AKTORID")
        val pdlAktørMedPersonIdent = Aktør(pdlAktør.aktørId)
        pdlAktørMedPersonIdent.personidenter.add(Personident(pdlFødselsnummer, pdlAktør))

        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns null
        every { personidentRepository.findByFødselsnummerOrNull(pdlFødselsnummer) } returns null
        every { aktørRepository.findByAktørId(fødselsnummer) } returns null
        every { aktørRepository.findByAktørId(pdlAktør.aktørId) } returns null
        every { pdlClient.hentIdenter(any(), false) } returns listOf(personIdentPDL, aktørIdentPDL)
        every { aktørRepository.saveAndFlush(pdlAktørMedPersonIdent) } returns pdlAktørMedPersonIdent

        val hentetAktør = personidentService.hentOgLagreAktør(fødselsnummer, true)

        verify(exactly = 1) { aktørRepository.saveAndFlush(pdlAktørMedPersonIdent) }

        assertEquals(pdlAktørMedPersonIdent, hentetAktør)
    }

    @Test
    fun `hentAktør - skal hente aktør dersom aktør har en aktiv personident`() {
        val fødselsnummer = randomFnr()
        val personIdent = Personident(fødselsnummer, randomAktør(fødselsnummer), aktiv = true)
        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns personIdent

        val hentetAktør = personidentService.hentAktør(fødselsnummer)

        assertEquals(fødselsnummer, hentetAktør.personidenter.first { it.aktiv }.fødselsnummer)
    }

    @Test
    fun `hentAktør - skal kaste Feil dersom aktør ikke har en aktiv personident`() {
        val fødselsnummer = randomFnr()
        val aktør = Aktør(randomAktørId())
        val personIdent = Personident(fødselsnummer, aktør, aktiv = false)
        aktør.personidenter.add(personIdent)

        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns personIdent

        val feil = assertThrows<Feil> { personidentService.hentAktør(fødselsnummer) }

        assertEquals("Fant ikke aktiv ident for aktør", feil.message)
    }
}
