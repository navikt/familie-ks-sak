package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.data.lagBrevmottakerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrevmottakerDtoKtTest {
    @Test
    fun `harGyldigAdresse skal returnere true hvis brevmottakeren er norsk og har postnummer og poststed satt`() {
        // Arrange
        val brevmottaker = lagBrevmottakerDto(id = 12343, postnummer = "0661", poststed = "Oslo", landkode = "NO")

        // Act
        val beslutning = brevmottaker.harGyldigAdresse()

        // Assert
        assertThat(beslutning).isTrue()
    }

    @Test
    fun `harGyldigAdresse skal returnere true hvis brevmottakeren er utenlandsk og har postnummer og poststed satt til tomme strenger`() {
        // Arrange
        val brevmottaker = lagBrevmottakerDto(id = 12343, postnummer = "", poststed = "", landkode = "SE")

        // Act
        val beslutning = brevmottaker.harGyldigAdresse()

        // Assert
        assertThat(beslutning).isTrue()
    }

    @Test
    fun `harGyldigAdresse skal returnere false hvis brevmottakeren er norsk og har postnummer og poststed satt til tomme strenger`() {
        // Arrange
        val brevmottaker = lagBrevmottakerDto(id = 12343, postnummer = "", poststed = "", landkode = "NO")

        // Act
        val beslutning = brevmottaker.harGyldigAdresse()

        // Assert
        assertThat(beslutning).isFalse()
    }

    @Test
    fun `harGyldigAdresse skal returnere false hvis brevmottakeren er utenlandsk og har postnummer og poststed satt`() {
        // Arrange
        val brevmottaker = lagBrevmottakerDto(id = 12343, postnummer = "0000", poststed = "Stockholm", landkode = "SE")

        // Act
        val beslutning = brevmottaker.harGyldigAdresse()

        // Assert
        assertThat(beslutning).isFalse()
    }
}
