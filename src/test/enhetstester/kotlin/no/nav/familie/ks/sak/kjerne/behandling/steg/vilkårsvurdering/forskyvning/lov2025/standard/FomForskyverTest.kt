package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2025.standard

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.standard.forskyvFom
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FomForskyverTest {
    @Test
    fun `skal returnere null om input fom dato er null`() {
        // Act
        val forskjøvetFomDato = forskyvFom(null)

        // Assert
        assertThat(forskjøvetFomDato).isNull()
    }

    @Test
    fun `skal forskyve fom dato til første dag i neste måned om input dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2025, 1, 1)

        // Act
        val forskjøvetFomDato = forskyvFom(fomDato)

        // Assert
        assertThat(forskjøvetFomDato).isEqualTo(LocalDate.of(2025, 2, 1))
    }

    @Test
    fun `skal forskyve fom dato til første dag i neste måned om input dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetFomDato = forskyvFom(fomDato)

        // Assert
        assertThat(forskjøvetFomDato).isEqualTo(LocalDate.of(2025, 2, 1))
    }
}
