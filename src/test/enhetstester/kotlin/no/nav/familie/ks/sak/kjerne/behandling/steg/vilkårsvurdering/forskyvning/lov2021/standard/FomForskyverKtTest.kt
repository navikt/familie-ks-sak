package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2021.standard

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2021.standard.forskyvFom
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FomForskyverKtTest {
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
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFomDato = forskyvFom(fomDato)

        // Assert
        assertThat(forskjøvetFomDato).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato til første dag i neste måned om input dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFomDato = forskyvFom(fomDato)

        // Assert
        assertThat(forskjøvetFomDato).isEqualTo(LocalDate.of(2024, 8, 1))
    }
}
