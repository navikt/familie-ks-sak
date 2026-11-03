package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.datagenerator.lagGrUkjentAdresseOppholdsadresse
import no.nav.familie.ks.sak.datagenerator.lagGrVegadresseBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.opphold.GrOpphold
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonTest {
    @Nested
    inner class PersonopplysningerErLike {
        private val aktør = randomAktør()
        private val periode = DatoIntervallEntitet(fom = LocalDate.of(2020, 1, 1), tom = null)

        private fun lagPersonMedRegisteropplysninger(id: Long): Person =
            lagPerson(
                id = id,
                type = PersonType.SØKER,
                fødselsdato = LocalDate.of(1990, 1, 1),
                navn = "Navn",
                personopplysningGrunnlag = PersonopplysningGrunnlag(id = id, behandlingId = 1),
                aktør = aktør,
                bostedsadresser = { listOf(lagGrVegadresseBostedsadresse().medPeriode(periode)) },
                oppholdsadresser = { listOf(lagGrUkjentAdresseOppholdsadresse(periode = periode)) },
                statsborgerskap = { listOf(GrStatsborgerskap(id = id, gyldigPeriode = periode, landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = it)) },
                opphold = { listOf(GrOpphold(id = id, gyldigPeriode = periode, type = OPPHOLDSTILLATELSE.PERMANENT, person = it)) },
                arbeidsforhold = { listOf(GrArbeidsforhold(id = id, periode = periode, arbeidsgiverId = "123", arbeidsgiverType = "Organisasjon", person = it)) },
                sivilstander = { listOf(GrSivilstand(id = id, fom = LocalDate.of(2015, 1, 1), type = SIVILSTANDTYPE.GIFT, person = it)) },
            ).also {
                it.dødsfall = lagDødsfall(id = id, person = it)
            }

        private fun lagDødsfall(
            id: Long,
            person: Person,
        ) = Dødsfall(
            id = id,
            person = person,
            dødsfallDato = LocalDate.of(2024, 1, 1),
            dødsfallAdresse = "Adresse",
            dødsfallPostnummer = "0001",
            dødsfallPoststed = "Oslo",
        )

        private fun GrBostedsadresse.medPeriode(nyPeriode: DatoIntervallEntitet) = also { it.periode = nyPeriode }

        private val person = lagPersonMedRegisteropplysninger(id = 1)

        @Test
        fun `skal returnere true når alle felter er identiske selv om id-ene er forskjellige`() {
            assertThat(person.personopplysningerErLike(lagPersonMedRegisteropplysninger(id = 2))).isTrue()
        }

        @Test
        fun `skal returnere false når aktør er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).copy(aktør = randomAktør())

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når navn er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).copy(navn = "Annet Navn")

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når fødselsdato er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).copy(fødselsdato = LocalDate.of(1991, 1, 1))

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når kjønn er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).copy(kjønn = Kjønn.MANN)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når målform er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).copy(målform = Målform.NN)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når type er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).copy(type = PersonType.BARN)

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når dødsfall er forskjellig`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { dødsfall = null }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når opphold er forskjellige`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { opphold = mutableListOf() }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når bostedsadresser er forskjellige`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { bostedsadresser = mutableListOf() }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når person har flyttet til ny leilighet i samme bygg`() {
            // Arrange
            val nyPerson =
                lagPersonMedRegisteropplysninger(id = 2).apply {
                    bostedsadresser = mutableListOf(lagGrVegadresseBostedsadresse().copy(bruksenhetsnummer = "2").medPeriode(periode))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når perioden på en adresse er endret`() {
            // Arrange
            val nyPerson =
                lagPersonMedRegisteropplysninger(id = 2).apply {
                    bostedsadresser = mutableListOf(lagGrVegadresseBostedsadresse().medPeriode(DatoIntervallEntitet(fom = LocalDate.of(2020, 2, 1), tom = null)))
                }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når oppholdsadresser er forskjellige`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { oppholdsadresser = mutableListOf() }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når sivilstander er forskjellige`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { sivilstander = mutableListOf() }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når statsborgerskap er forskjellige`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { statsborgerskap = mutableListOf() }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }

        @Test
        fun `skal returnere false når arbeidsforhold er forskjellige`() {
            // Arrange
            val nyPerson = lagPersonMedRegisteropplysninger(id = 2).apply { arbeidsforhold = mutableListOf() }

            // Act & Assert
            assertThat(person.personopplysningerErLike(nyPerson)).isFalse()
        }
    }
}
