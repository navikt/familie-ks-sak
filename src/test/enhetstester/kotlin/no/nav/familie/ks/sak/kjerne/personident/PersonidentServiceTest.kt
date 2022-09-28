package no.nav.familie.ks.sak.kjerne.personident

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.pdl.PdlClient
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlIdent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonidentServiceTest {

    @MockK
    private lateinit var personidentRepository: PersonidentRepository

    @MockK
    private lateinit var aktørRepository: AktørRepository

    @MockK
    private lateinit var pdlClient: PdlClient

    @InjectMockKs
    private lateinit var personidentService: PersonidentService

    @BeforeEach
    fun beforeEach() {
        every { personidentRepository.findByFødselsnummerOrNull(any()) } returns null
        every { pdlClient.hentIdenter(any(), false) } returns emptyList()
        every { aktørRepository.findByAktørId(any()) } returns null
    }

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
        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns null
        every { aktørRepository.findByAktørId(aktør.aktørId) } returns aktør

        val hentetAktør = personidentService.hentOgLagreAktør(fødselsnummer, true)
        assertNotNull(hentetAktør)
    }

    @Test
    fun `hentOgLagreAktør - skal hente personident fra personidentRepository dersom aktivt fødselsnummer fra PDL finnes i db`() {
        val fødselsnummer = randomFnr()
        val aktør = randomAktør(fødselsnummer)

        val pdlFødselsnummer = randomFnr()
        val pdlIdent = PdlIdent(pdlFødselsnummer, false, "FOLKEREGISTERIDENT")
        val personident = Personident(pdlFødselsnummer,)

        every { personidentRepository.findByFødselsnummerOrNull(fødselsnummer) } returns null
        every { personidentRepository.findByFødselsnummerOrNull(pdlFødselsnummer) } returns null
        every { aktørRepository.findByAktørId(fødselsnummer) } returns null
        every { pdlClient.hentIdenter(any(), false) } returns listOf(pdlIdent)

        val hentetAktør = personidentService.hentOgLagreAktør(fødselsnummer, true)

    }

    fun `hentOgLagreAktør - skal hente aktør fra aktørRepository dersom aktiv aktørId fra PDL finnes i db`() {
    }

    fun `hentOgLagreAktør - skal opprette aktør og personident med aktørId og personident fra PDL dersom verken aktørId eller fødselsnummer fra PDL finnes i db fra før`() {
    }
}
