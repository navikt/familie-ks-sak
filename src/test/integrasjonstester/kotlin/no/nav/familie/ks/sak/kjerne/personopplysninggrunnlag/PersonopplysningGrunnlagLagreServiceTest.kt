package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PersonopplysningGrunnlagLagreServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var personopplysningGrunnlagLagreService: PersonopplysningGrunnlagLagreService

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Test
    fun `skal lagre grunnlag når behandlingen ikke har grunnlag fra før`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()

        // Act
        val lagretGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandling.id, søker))

        // Assert
        assertThat(lagretGrunnlag.id).isNotZero()
        assertThat(personopplysningGrunnlagRepository.findById(lagretGrunnlag.id)).isPresent
        assertThat(personRepository.findById(lagretGrunnlag.personer.single().id)).isPresent
        assertThat(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)?.id).isEqualTo(lagretGrunnlag.id)
    }

    @Test
    fun `skal slette gammelt grunnlag før nytt grunnlag lagres`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        val gammeltGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandling.id, søker))
        val gammelPersonId = gammeltGrunnlag.personer.single().id

        // Act
        val nyttGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandling.id, søker))

        // Assert
        assertThat(personopplysningGrunnlagRepository.findById(gammeltGrunnlag.id)).isEmpty
        assertThat(personRepository.findById(gammelPersonId)).isEmpty
        assertThat(personopplysningGrunnlagRepository.findById(nyttGrunnlag.id)).isPresent
        assertThat(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)?.id).isEqualTo(nyttGrunnlag.id)
    }

    @Test
    fun `skal beholde gammelt grunnlag når lagring av nytt grunnlag feiler`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        val gammeltGrunnlag = personopplysningGrunnlagLagreService.lagreOgSlettGammelt(lagGrunnlag(behandling.id, søker))
        val gammelPersonId = gammeltGrunnlag.personer.single().id
        val ulagretAktør = randomAktør()
        val ugyldigNyttGrunnlag = lagGrunnlag(behandling.id, ulagretAktør)

        // Act & Assert
        assertThrows<RuntimeException> {
            personopplysningGrunnlagLagreService.lagreOgSlettGammelt(ugyldigNyttGrunnlag)
        }
        assertThat(personopplysningGrunnlagRepository.findById(gammeltGrunnlag.id)).isPresent
        assertThat(personRepository.findById(gammelPersonId)).isPresent
        assertThat(personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id)?.id).isEqualTo(gammeltGrunnlag.id)
    }

    private fun lagGrunnlag(
        behandlingId: Long,
        aktør: Aktør,
    ): PersonopplysningGrunnlag =
        PersonopplysningGrunnlag(behandlingId = behandlingId).apply {
            personer.add(
                Person(
                    type = PersonType.SØKER,
                    personopplysningGrunnlag = this,
                    aktør = aktør,
                    fødselsdato = LocalDate.of(1990, 1, 1),
                    navn = "",
                    kjønn = Kjønn.KVINNE,
                ),
            )
        }
}
