package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lov2021.barnehageplass

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2021.barnehageplass.Graderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2021.barnehageplass.forskyvTomBasertPåGraderingsforskjell
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2021.barnehageplass.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TomForskyverKtTest {
    @Test
    fun `skal forskyve dagens dato via extension function`() {
        // Arrange
        val dagensDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetTom = dagensDato.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør()

        // Assert
        assertThat(forskjøvetTom).isEqualTo(YearMonth.of(2024, 7))
    }

    @Test
    fun `skal forskyve dagens dato via extension function hvis den er null`() {
        // Arrange
        val dagensDato = null

        // Act
        val forskjøvetTom = dagensDato.tilForskjøvetTomMånedForSisteUtbetalingsperiodePgaFremtidigOpphør()

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
        val tomDato = LocalDate.of(2024, 7, 31)

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
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 31)

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
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FULL_BARNEHAGEPLASS_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell ØKNING_GRUNNET_SLUTT_I_BARNEHAGE til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 31)

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
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.ØKNING_GRUNNET_SLUTT_I_BARNEHAGE,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell ØKNING til siste dag i måneden når tom dato allerede er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 31)

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
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING til siste dag i neste måneden når tom dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 8, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING til siste dag i måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.INGEN_UTBETALING_GRUNNET_FØRSTE_PERIODE_TIL_ØKNING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING til siste dag i måneden når tom dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON_GÅR_TIL_INGEN_UTBETALING til siste dag i forrige måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON_GÅR_TIL_INGEN_UTBETALING,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 6, 30))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON til siste dag i måneden når tom dato er siste dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 31)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 7, 31))
    }

    @Test
    fun `skal forskyve tom dato for graderingsforskjell REDUKSJON til siste dag i forrige måneden når tom dato er første dag i måneden`() {
        // Arrange
        val tomDato = LocalDate.of(2024, 7, 1)

        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell(
                tomDato,
                Graderingsforskjell.REDUKSJON,
            )

        // Assert
        assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 6, 30))
    }
}
