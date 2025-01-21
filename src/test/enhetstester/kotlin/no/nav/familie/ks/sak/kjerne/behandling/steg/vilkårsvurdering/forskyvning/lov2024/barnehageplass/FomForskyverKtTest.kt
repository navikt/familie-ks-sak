package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2024.barnehageplass

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass.Graderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.barnehageplass.forskyvFomBasertPåGraderingsforskjell2024
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
    @EnumSource(
        value = Graderingsforskjell::class,
        names = [
            "REDUKSJON",
            "ØKNING_FRA_FULL_BARNEHAGEPLASS",
        ],
        mode = EnumSource.Mode.EXCLUDE,
    )
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

    @Test
    fun `skal forskyve fom dato til første dag neste månede når fom dato er første dag i måneden ved reduksjon`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 10, 1)

        // Act
        val forskjøvetDato =
            forskyvFomBasertPåGraderingsforskjell2024(
                fomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetDato).isEqualTo(LocalDate.of(2024, 11, 1))
    }

    @Test
    fun `skal forskyve fom dato til første dag neste månede når fom dato er siste dag i måneden ved reduksjon`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 10, 31)

        // Act
        val forskjøvetDato =
            forskyvFomBasertPåGraderingsforskjell2024(
                fomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetDato).isEqualTo(LocalDate.of(2024, 11, 1))
    }

    @Test
    fun `skal forskyve fom dato til første dag samme månede når fom dato er første dag i måneden ved økning fra full barnehageplass`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 10, 1)

        // Act
        val forskjøvetDato =
            forskyvFomBasertPåGraderingsforskjell2024(
                fomDato,
                Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS,
            )

        // Assert
        assertThat(forskjøvetDato).isEqualTo(LocalDate.of(2024, 10, 1))
    }

    @Test
    fun `skal forskyve fom dato til første dag neste månede når fom dato er siste dag i måneden ved økning fra full barnehageplass`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 10, 31)

        // Act
        val forskjøvetDato =
            forskyvFomBasertPåGraderingsforskjell2024(
                fomDato,
                Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS,
            )

        // Assert
        assertThat(forskjøvetDato).isEqualTo(LocalDate.of(2024, 11, 1))
    }
}
