package no.nav.familie.ks.sak.kjerne.fagsak.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomAktør
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @BeforeEach
    fun beforeEach() {
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE)
    }

    @Test
    fun `finnFagsak skal returnere fagsak dersom det eksisterer en fagsak med id`() {
        // Arrange (data er opprettet av @BeforeEach)

        // Act
        val hentetFagsak = fagsakRepository.finnFagsak(fagsak.id)!!

        // Assert
        assertThat(hentetFagsak.id, Is(fagsak.id))
        assertThat(hentetFagsak.aktør, Is(fagsak.aktør))
    }

    @Test
    fun `finnFagsak skal returnere null dersom det ikke eksisterer en fagsak med id`() {
        // Arrange

        // Act
        val ikkeEksisterendeFagsak = fagsakRepository.finnFagsak(404)

        // Assert
        assertThat(ikkeEksisterendeFagsak, Is(nullValue()))
    }

    @Test
    fun `finnFagsakForAktør skal returnere null dersom det ikke finnes fagsak for aktør`() {
        // Arrange
        val randomAktør = randomAktør()

        // Act
        val ikkeEksisterendeFagsak = fagsakRepository.finnFagsakForAktør(randomAktør)

        // Assert
        assertThat(ikkeEksisterendeFagsak, Is(nullValue()))
    }

    @Test
    fun `finnFagsakForAktør skal returnere fagsak dersom det finnes fagsak for aktør`() {
        // Arrange (data er opprettet av @BeforeEach)

        // Act
        val hentetFagsak = fagsakRepository.finnFagsakForAktør(søker)!!

        // Assert
        assertThat(hentetFagsak.id, Is(fagsak.id))
        assertThat(hentetFagsak.aktør, Is(fagsak.aktør))
    }
}
