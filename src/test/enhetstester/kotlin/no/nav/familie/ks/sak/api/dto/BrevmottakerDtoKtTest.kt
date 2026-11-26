package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.data.lagBrevmottakerDto
import no.nav.familie.ks.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BrevmottakerDtoKtTest {
    @Nested
    inner class TilDomene {
        @Test
        fun `skal mappe feltene og bruke behandlingId uten å gjenbruke mottakerId`() {
            // Arrange
            val dto =
                lagBrevmottakerDto(
                    id = 5,
                    type = MottakerType.VERGE,
                    navn = "Verge Vergesen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = " 0661 ",
                    poststed = " Oslo ",
                    landkode = "NO",
                )

            // Act
            val brevmottaker = dto.tilDomene(behandlingId = 200)

            // Assert
            assertThat(brevmottaker).isEqualTo(
                lagBrevmottakerDb(
                    id = 0,
                    behandlingId = 200,
                    type = MottakerType.VERGE,
                    navn = "Verge Vergesen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = "0661",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
            )
        }

        @Test
        fun `skal bevare manglende adresselinje og tomt postnummer og poststed for utenlandsk adresse`() {
            // Arrange
            val dto = lagBrevmottakerDto(adresselinje2 = null, postnummer = "", poststed = "", landkode = "SE")

            // Act
            val brevmottaker = dto.tilDomene(behandlingId = 1)

            // Assert
            assertThat(brevmottaker.adresselinje2).isNull()
            assertThat(brevmottaker.postnummer).isEmpty()
            assertThat(brevmottaker.poststed).isEmpty()
            assertThat(brevmottaker.landkode).isEqualTo("SE")
        }
    }

    @Nested
    inner class Fra {
        @Test
        fun `skal mappe alle feltene fra lagret brevmottaker`() {
            // Arrange
            val brevmottaker =
                lagBrevmottakerDb(
                    id = 5,
                    type = MottakerType.VERGE,
                    navn = "Verge Vergesen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = "0661",
                    poststed = "Oslo",
                    landkode = "NO",
                )

            // Act
            val dto = BrevmottakerDto.fra(brevmottaker)

            // Assert
            assertThat(dto).isEqualTo(
                lagBrevmottakerDto(
                    id = 5,
                    type = MottakerType.VERGE,
                    navn = "Verge Vergesen",
                    adresselinje1 = "Adresse 1",
                    adresselinje2 = "Adresse 2",
                    postnummer = "0661",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
            )
        }
    }

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
