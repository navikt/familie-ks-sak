package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag

import jakarta.persistence.EntityManager
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.data.lagBostedsadresse
import no.nav.familie.ks.sak.kjerne.personident.AktørRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.opphold.GrOpphold
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

class PersonopplysningGrunnlagCascadeDeleteTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var aktørRepository: AktørRepository

    @Autowired
    private lateinit var personidentRepository: PersonidentRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `sletting av personopplysningsgrunnlag via JPA kaskaderer til person og registeropplysninger`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true)
        leggTilRegisteropplysningerPå(søkerPerson)

        val grunnlagId = personopplysningGrunnlag.id
        val søkerPersonId = søkerPerson.id
        val søkerAktørId = søker.aktørId
        val barnAktørId = barn.aktørId

        assertThat(antallRader("po_person", "fk_gr_personopplysninger_id", grunnlagId)).isEqualTo(2)
        registeropplysningstabeller.forEach { tabell ->
            assertThat(antallRader(tabell, "fk_po_person_id", søkerPersonId)).`as`(tabell).isEqualTo(1)
        }

        // Act
        val fersktGrunnlag = personopplysningGrunnlagRepository.findById(grunnlagId).orElseThrow()
        personopplysningGrunnlagRepository.delete(fersktGrunnlag)
        personopplysningGrunnlagRepository.flush()
        entityManager.clear()

        // Assert
        assertThat(personopplysningGrunnlagRepository.findById(grunnlagId)).isEmpty
        assertThat(antallRader("po_person", "fk_gr_personopplysninger_id", grunnlagId)).isZero()
        registeropplysningstabeller.forEach { tabell ->
            assertThat(antallRader(tabell, "fk_po_person_id", søkerPersonId)).`as`(tabell).isZero()
        }

        assertThat(aktørRepository.findById(søkerAktørId)).isPresent
        assertThat(aktørRepository.findById(barnAktørId)).isPresent
        assertThat(personidentRepository.hentAlleIdenterForAktørid(søkerAktørId)).isNotEmpty()
    }

    @Test
    @Transactional
    fun `native DELETE fra gr_personopplysninger kaskaderer til person og registeropplysninger`() {
        // Arrange
        opprettSøkerFagsakOgBehandling()
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true)
        leggTilRegisteropplysningerPå(søkerPerson)

        val grunnlagId = personopplysningGrunnlag.id
        val søkerPersonId = søkerPerson.id
        val søkerAktørId = søker.aktørId

        assertThat(antallRader("po_person", "fk_gr_personopplysninger_id", grunnlagId)).isEqualTo(2)
        registeropplysningstabeller.forEach { tabell ->
            assertThat(antallRader(tabell, "fk_po_person_id", søkerPersonId)).`as`(tabell).isEqualTo(1)
        }

        // Act
        entityManager
            .createNativeQuery("DELETE FROM gr_personopplysninger WHERE id = :id")
            .setParameter("id", grunnlagId)
            .executeUpdate()

        // Assert
        assertThat(antallRader("po_person", "fk_gr_personopplysninger_id", grunnlagId)).isZero()
        registeropplysningstabeller.forEach { tabell ->
            assertThat(antallRader(tabell, "fk_po_person_id", søkerPersonId)).`as`(tabell).isZero()
        }

        assertThat(aktørRepository.findById(søkerAktørId)).isPresent
    }

    private val registeropplysningstabeller =
        listOf(
            "po_bostedsadresse",
            "po_statsborgerskap",
            "po_sivilstand",
            "po_opphold",
            "po_arbeidsforhold",
            "po_doedsfall",
        )

    private fun leggTilRegisteropplysningerPå(person: Person) {
        person.bostedsadresser = mutableListOf(GrBostedsadresse.fraBostedsadresse(lagBostedsadresse(), person))
        person.opphold = mutableListOf(GrOpphold(type = OPPHOLDSTILLATELSE.PERMANENT, person = person))
        person.arbeidsforhold =
            mutableListOf(
                GrArbeidsforhold(
                    arbeidsgiverId = "999999999",
                    arbeidsgiverType = "ORGANISASJON",
                    person = person,
                ),
            )
        person.dødsfall =
            Dødsfall(
                person = person,
                dødsfallDato = LocalDate.now().minusDays(1),
                dødsfallAdresse = null,
                dødsfallPostnummer = null,
                dødsfallPoststed = null,
            )
        personRepository.saveAndFlush(person)
    }

    private fun antallRader(
        tabell: String,
        kolonne: String,
        verdi: Any,
    ): Long =
        (
            entityManager
                .createNativeQuery("SELECT count(*) FROM $tabell WHERE $kolonne = :verdi")
                .setParameter("verdi", verdi)
                .singleResult as Number
        ).toLong()
}
