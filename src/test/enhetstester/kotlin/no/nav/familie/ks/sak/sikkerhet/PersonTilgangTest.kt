package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.tilgangsmaskin.Avvisningskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonTilgangTest {
    @Test
    fun `skal ha tilgang uten avvisning`() {
        // Act
        val tilgang = PersonTilgang.medTilgang(PERSONIDENT)

        // Assert
        assertThat(tilgang.harTilgang).isTrue
        assertThat(tilgang.avvisning).isNull()
    }

    @Test
    fun `skal ikke ha tilgang med avvisning`() {
        // Act
        val tilgang = PersonTilgang.avvist(PERSONIDENT, Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE)

        // Assert
        assertThat(tilgang.harTilgang).isFalse
        assertThat(tilgang.avvisning).isEqualTo(PersonTilgang.Avvisning(Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE))
    }

    @Test
    fun `skal maskere personidenten i toString`() {
        // Arrange
        val tilgang = PersonTilgang.avvist(PERSONIDENT, Avvisningskode.AVVIST_SKJERMING, BEGRUNNELSE)

        // Act
        val tekst = tilgang.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT)
        assertThat(tekst).contains("avvisningskode=AVVIST_SKJERMING")
    }

    companion object {
        private const val PERSONIDENT = "12345678910"
        private const val BEGRUNNELSE = "Du har ikke tilgang til Nav-ansatte og deres nærmeste familie"
    }
}
