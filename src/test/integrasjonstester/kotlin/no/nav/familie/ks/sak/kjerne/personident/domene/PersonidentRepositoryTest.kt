package no.nav.familie.ks.sak.kjerne.personident.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
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
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `hentAlleIdenterForAktørid - skal hente liste over alle personidenter tilknyttet aktørId`() {
        // Arrange (data er opprettet av @BeforeEach)

        // Act
        val hentedePersonidenter = personidentRepository.hentAlleIdenterForAktørid(søker.aktørId)

        // Assert
        assertEquals(1, hentedePersonidenter.size)
    }

    @Test
    fun `findByFødselsnummerOrNull - skal hente personident med bestemt fødselsnummer dersom det finnes i db`() {
        // Arrange
        val aktivFødselsnummer = søker.aktivFødselsnummer()

        // Act
        val personident = personidentRepository.findByFødselsnummerOrNull(aktivFødselsnummer)

        // Assert
        assertNotNull(personident)
        assertEquals(aktivFødselsnummer, personident!!.fødselsnummer)
    }

    @Test
    fun `findByFødselsnummerOrNull - skal returnere null dersom det ikke finnes en personident med bestemt fødselsnummer i db`() {
        // Arrange
        val fødselsnummer = randomFnr()

        // Act
        val personident = personidentRepository.findByFødselsnummerOrNull(fødselsnummer)

        // Assert
        assertNull(personident)
    }
}
