package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2025.barnehageplass

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass.Graderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass.forskyvTomBasertPåGraderingsforskjell
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.barnehageplass.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2025

class TomForskyverTest {
    @Test
    fun `skal forskyve dagens dato via extension function`() {
        // Arrange
        val dagensDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom = dagensDato.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2025()

        // Assert
        assertThat(forskjøvetTom).isEqualTo(YearMonth.of(2025, 1))
    }

    @Test
    fun `skal forskyve dagens dato via extension function hvis den er null`() {
        // Arrange
        val dagensDato = null

        // Act
        val forskjøvetTom = dagensDato.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør2025()

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Test
    fun `skal returnere null om tom dato er null`() {
        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                null,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell LIK til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(tomDato)
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell LIK til siste dag i måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(tomDato)
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING til siste dag i måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell ØKNING_GRUNNET_SLUTT_I_BARNEHAGE til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(tomDato)
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell ØKNING_GRUNNET_SLUTT_I_BARNEHAGE til siste dag i måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell ØKNING til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(tomDato)
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell ØKNING til siste dag i måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING til siste dag i neste måneden når tom dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 2, 28))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING til siste dag i måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING til siste dag i måneden når tom dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING til siste dag i forrige måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 2, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON til siste dag i måneden når tom dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 1, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON til siste dag i forrige måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2025, 2, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2025, 1, 31))
    }
}
