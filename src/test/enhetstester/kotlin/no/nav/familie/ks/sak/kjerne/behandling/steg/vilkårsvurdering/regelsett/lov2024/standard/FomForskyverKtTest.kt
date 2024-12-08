package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FomForskyverKtTest {
    @Test
    fun `skal returnere null når input fom dato er null`() {
        // Act
        val forskjøvetFom = forskyvFom(null)

        // Assert
        assertThat(forskjøvetFom).isNull()
    }

    @Test
    fun `skal forskyve fom til første dag i nåværende måned når fom er første dag i nåværende måned`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 1)

        // Act
        val forskjøvetFom = forskyvFom(dagensDato)

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 11, 1))
    }

    @Test
    fun `skal forskyve fom til første dag i nåværende måned når fom er siste dag i nåværende måned`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 11, 30)

        // Act
        val forskjøvetFom = forskyvFom(dagensDato)

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 11, 1))
    }
}
