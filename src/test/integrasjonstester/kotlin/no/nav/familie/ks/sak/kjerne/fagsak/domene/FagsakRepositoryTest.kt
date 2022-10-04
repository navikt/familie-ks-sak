package no.nav.familie.ks.sak.kjerne.fagsak.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private lateinit var søker: Aktør

    @BeforeEach
    fun beforeEach() {
        søker = lagreAktør(randomAktør())
    }

    @Test
    fun `finnFagsak skal returnere fagsak dersom det eksisterer en fagsak med id`() {
        val fagsak = lagreFagsak(lagFagsak(søker))

        val hentetFagsak = fagsakRepository.finnFagsak(fagsak.id)!!

        assertThat(hentetFagsak.id, Is(fagsak.id))
        assertThat(hentetFagsak.aktør, Is(fagsak.aktør))
    }

    @Test
    fun `finnFagsak skal returnere null dersom det ikke eksisterer en fagsak med id`() {
        val ikkeEksisterendeFagsak = fagsakRepository.finnFagsak(404)

        assertThat(ikkeEksisterendeFagsak, Is(nullValue()))
    }

    @Test
    fun `finnFagsakForAktør skal returnere null dersom det ikke finnes fagsak for aktør`() {
        val randomAktør = randomAktør()
        val ikkeEksisterendeFagsak = fagsakRepository.finnFagsakForAktør(randomAktør)

        assertThat(ikkeEksisterendeFagsak, Is(nullValue()))
    }

    @Test
    fun `finnFagsakForAktør skal returnere fagsak dersom det finnes fagsak for aktør`() {
        val fagsak = lagreFagsak(lagFagsak(søker))

        val hentetFagsak = fagsakRepository.finnFagsakForAktør(søker)!!

        assertThat(hentetFagsak.id, Is(fagsak.id))
        assertThat(hentetFagsak.aktør, Is(fagsak.aktør))
    }
}
