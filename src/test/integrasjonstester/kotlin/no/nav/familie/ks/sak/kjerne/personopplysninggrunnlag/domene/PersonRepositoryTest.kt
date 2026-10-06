package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PersonRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `finnPersonerIAktiveGrunnlag skal returnere person fra nyeste grunnlag først`() {
        // Arrange
        opprettSøkerFagsakOgBehandling(behandlingStatus = BehandlingStatus.AVSLUTTET)
        val eldsteGrunnlag = lagreGrunnlagMedSøker(behandling.id)

        behandling.aktiv = false
        lagreBehandling(behandling)
        val nyBehandling = lagreBehandling(lagBehandling(fagsak = fagsak, type = BehandlingType.REVURDERING))
        val nyesteGrunnlag = lagreGrunnlagMedSøker(nyBehandling.id)

        // Act
        val treff = personRepository.finnPersonerIAktiveGrunnlag(søker)

        // Assert
        assertThat(treff.map { it.personopplysningGrunnlag.id }).containsExactly(nyesteGrunnlag.id, eldsteGrunnlag.id)
    }

    @Test
    fun `finnPersonerIAktiveGrunnlag skal returnere personer for aktør`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling()

        // Act
        val treff = personRepository.finnPersonerIAktiveGrunnlag(søker)

        // Assert
        assertThat(treff).hasSize(1)
        assertThat(treff.single().personopplysningGrunnlag.id).isEqualTo(personopplysningGrunnlag.id)
    }

    @Test
    fun `finnPersonerIAktiveGrunnlag skal returnere tom liste dersom aktør ikke finnes i noe grunnlag`() {
        // Arrange
        val aktør = randomAktør()

        // Act
        val treff = personRepository.finnPersonerIAktiveGrunnlag(aktør)

        // Assert
        assertThat(treff).isEmpty()
    }

    @Test
    fun `findFagsakerByAktør skal returnere fagsak dersom aktør finnes i grunnlag`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling()

        // Act
        val fagsaker = personRepository.findFagsakerByAktør(søker)

        // Assert
        assertThat(fagsaker).containsExactly(fagsak)
    }

    @Test
    fun `findFagsakerByAktør skal ikke returnere arkivert fagsak`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling()

        fagsak.arkivert = true
        lagreFagsak(fagsak)

        // Act
        val fagsaker = personRepository.findFagsakerByAktør(søker)

        // Assert
        assertThat(fagsaker).isEmpty()
    }

    private fun lagreGrunnlagMedSøker(behandlingId: Long): PersonopplysningGrunnlag {
        val grunnlag = personopplysningGrunnlagRepository.saveAndFlush(PersonopplysningGrunnlag(behandlingId = behandlingId))
        lagrePerson(
            Person(
                aktør = søker,
                type = PersonType.SØKER,
                personopplysningGrunnlag = grunnlag,
                fødselsdato = LocalDate.of(2000, 1, 1),
                navn = "",
                kjønn = Kjønn.KVINNE,
            ),
        )
        return grunnlag
    }
}
