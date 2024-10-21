package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class FomForskyverKtTest {
    @Test
    fun `skal returnere null om fom dato er null`() {
        // Act
        val forskjøvetDato =
            forskyvFomBasertPåGraderingsforskjell2024(
                null,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetDato).isNull()
    }

    @ParameterizedTest
    @EnumSource(value = Graderingsforskjell::class)
    fun `skal forskyve fom til første dag i inneværende måned om fom ikke er null`(
        graderingsforskjell: Graderingsforskjell,
    ) {
        // Arrange
        val fomDato = LocalDate.of(2024, 10, 21)

        // Act
        val forskjøvetDato =
            forskyvFomBasertPåGraderingsforskjell2024(
                fomDato,
                graderingsforskjell,
            )

        // Assert
        assertThat(forskjøvetDato).isEqualTo(fomDato.førsteDagIInneværendeMåned())
    }
}
