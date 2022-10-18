package no.nav.familie.ks.sak.kjerne.personident.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.personident.PersonidentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonidentRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var personidentRepository: PersonidentRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling()
    }

    @Test
    fun `hentAlleIdenterForAktørid - skal hente liste over alle personidenter tilknyttet aktørId`() {
        val hentedePersonidenter = personidentRepository.hentAlleIdenterForAktørid(søker.aktørId)

        assertEquals(1, hentedePersonidenter.size)
    }

    @Test
    fun `findByFødselsnummerOrNull - skal hente personident med bestemt fødselsnummer dersom det finnes i db`() {
        val aktivFødselsnummer = søker.aktivFødselsnummer()

        val personident = personidentRepository.findByFødselsnummerOrNull(aktivFødselsnummer)

        assertNotNull(personident)
        assertEquals(aktivFødselsnummer, personident!!.fødselsnummer)
    }

    @Test
    fun `findByFødselsnummerOrNull - skal returnere null dersom det ikke finnes en personident med bestemt fødselsnummer i db`() {
        val fødselsnummer = randomFnr()

        val personident = personidentRepository.findByFødselsnummerOrNull(fødselsnummer)

        assertNull(personident)
    }
}
