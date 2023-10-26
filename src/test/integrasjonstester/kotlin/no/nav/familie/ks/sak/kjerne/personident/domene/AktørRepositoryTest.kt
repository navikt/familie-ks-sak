package no.nav.familie.ks.sak.kjerne.personident.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomAktørId
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AktørRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling()
    }

    @Test
    fun `findByAktørId - skal returnere Aktør dersom aktør med bestemt aktørId finnes i db`() {
        val aktør = opprettAktør()

        val hentetAktør = aktørRepository.findByAktørId(aktør.aktørId)

        assertNotNull(hentetAktør)
    }

    @Test
    fun `findByAktørId - skal returnere null dersom aktør med bestemt aktørId ikke finnes i db`() {
        val aktørId = randomAktørId()

        val hentetAktør = aktørRepository.findByAktørId(aktørId)

        assertNull(hentetAktør)
    }

    private fun opprettAktør(): Aktør {
        return aktørRepository.saveAndFlush(randomAktør())
    }
}
