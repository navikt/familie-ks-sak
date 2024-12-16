package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TomForskyverKtTest {
    @Test
    fun `skal returnere null når input dato er null`() {
        // Act
        val forskjøvetTom = forskyvTom(null)

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Test
    fun `skal returnere siste dag i måneden når input dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 8, 1)

        // Act
        val forskjøvetTom = forskyvTom(tomDato)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 8, 31))
    }

    @Test
    fun `skal returnere siste dag i måneden når input dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 8, 31)

        // Act
        val forskjøvetTom = forskyvTom(tomDato)

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 8, 31))
    }
}
