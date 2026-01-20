package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser

import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.datagenerator.lagGrMatrikkeladresseBostedsadresse
import no.nav.familie.ks.sak.datagenerator.lagGrUkjentBostedBostedsadresse
import no.nav.familie.ks.sak.datagenerator.lagGrVegadresseBostedsadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AdresserTest {
    @Nested
    inner class OpprettFra {
        @Test
        fun `skal opprette adresser fra person med bostedsadresser`() {
            // Arrange
            val grVegadresse = lagGrVegadresseBostedsadresse()
            val grMatrikkeladresse = lagGrMatrikkeladresseBostedsadresse()
            val grUkjentBosted = lagGrUkjentBostedBostedsadresse()
            val person =
                lagPerson().apply {
                    this.bostedsadresser.clear()
                    this.bostedsadresser.addAll(listOf(grVegadresse, grMatrikkeladresse, grUkjentBosted))
                }

            // Act
            val adresser = Adresser.opprettFra(person)

            // Assert
            assertThat(adresser.bostedsadresser).hasSize(3)

            val vegadresse = adresser.bostedsadresser.single { it.vegadresse != null }.vegadresse
            val matrikkeladresse = adresser.bostedsadresser.single { it.matrikkeladresse != null }.matrikkeladresse
            val ukjentBosted = adresser.bostedsadresser.single { it.ukjentBosted != null }.ukjentBosted
            assertThat(vegadresse).isEqualTo(grVegadresse.tilAdresse().vegadresse)
            assertThat(matrikkeladresse).isEqualTo(grMatrikkeladresse.tilAdresse().matrikkeladresse)
            assertThat(ukjentBosted).isEqualTo(grUkjentBosted.tilAdresse().ukjentBosted)
            assertThat(adresser.oppholdsadresse).isEmpty()
        }
    }
}
