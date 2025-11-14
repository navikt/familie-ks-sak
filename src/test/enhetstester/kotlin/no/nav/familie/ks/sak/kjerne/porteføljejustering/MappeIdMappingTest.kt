package no.nav.familie.ks.sak.kjerne.porteføljejustering

import no.nav.familie.ks.sak.common.exception.Feil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MappeIdMappingTest {
    @ParameterizedTest
    @CsvSource(
        "100012691, 100012789",
        "100012692, 100012790",
        "100012693, 100012791",
        "100012721, 100012792",
        "100012695, 100012765",
    )
    fun `skal returnere korrekt mappe id for Bergen når mappe id fra Vadsø finnes i mapping`(
        mappeIdVadsø: Int,
        forventetMappeIdBergen: Int,
    ) {
        // Act
        val result = hentMappeIdHosBergenSomTilsvarerMappeIVadsø(mappeIdVadsø)

        // Assert
        assertThat(result).isEqualTo(forventetMappeIdBergen)
    }

    @Test
    fun `skal kaste Feil når mappe id fra Vadsø ikke finnes i mapping`() {
        // Arrange
        val ugyldigMappeIdVadsø = 999999

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                hentMappeIdHosBergenSomTilsvarerMappeIVadsø(ugyldigMappeIdVadsø)
            }

        // Assert
        assertThat(feil.message).contains("Finner ikke mappe id $ugyldigMappeIdVadsø i mapping")
    }
}
