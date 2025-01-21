package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2021.barnehageplass

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2021.barnehageplass.Graderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.regelverkFørFebruar2025.lov2021.barnehageplass.forskyvFomBasertPåGraderingsforskjell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FomForskyverKtTest {
    @Test
    fun `skal returnere null om fom dato er null`() {
        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                null,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetFom).isNull()
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell LIK til første dag i neste måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell LIK til første dag i måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell ØKNING_GRUNNET_SLUTT_I_BARNEHAGE til første dag i neste måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell ØKNING_GRUNNET_SLUTT_I_BARNEHAGE til første dag i måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell ØKNING til første dag i neste måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.ØKNING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell ØKNING til første dag i måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.ØKNING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING til første dag i neste måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING til første dag i neste måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING til første dag i neste måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING til første dag i neste måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 8, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING til første dag i måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING til første dag i måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell REDUKSJON til første dag i måneden når fom dato er siste dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }

    @Test
    fun `skal forskyve fom dato for graderingsforskjell REDUKSJON til første dag i måneden når fom dato er første dag i måneden`() {
        // Arrange
        val fomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetFom =
            forskyvFomBasertPåGraderingsforskjell(
                fomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetFom).isEqualTo(LocalDate.of(2024, 7, 1))
    }
}
