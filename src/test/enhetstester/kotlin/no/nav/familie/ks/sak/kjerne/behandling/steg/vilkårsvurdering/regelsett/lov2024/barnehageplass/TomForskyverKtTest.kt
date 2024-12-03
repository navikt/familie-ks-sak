package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.barnehageplass

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TomForskyverKtTest {
    @Test
    fun `skal returnere null når tom dato er null`() {
        // Act
        val forskjøvetTom =
            forskyvTomBasertPåGraderingsforskjell2024(
                null,
                Graderingsforskjell.LIK,
            )

        // Assert
        assertThat(forskjøvetTom).isNull()
    }

    @Nested
    inner class GraderingsforskjellLikTest {
        @Test
        fun `skal forskyve tom til siste dag i samme måned når tom dato er siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 31)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.LIK,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }

        @Test
        fun `skal forskyve tom til siste dag i forrige måned når tom dato er nest siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 30)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.LIK,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 9, 30))
        }

        @Test
        fun `skal forskyve tom til siste dag i forrige måned når tom dato er første dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 1)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.LIK,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 9, 30))
        }
    }

    @Nested
    inner class GraderingsforskjellReduksjonTest {
        @Test
        fun `skal forskyve tom til siste dag i neste måned når tom dato er siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 31)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.REDUKSJON,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 11, 30))
        }

        @Test
        fun `skal forskyve tom til siste dag i nåværende måned når tom dato er nest siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 30)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.REDUKSJON,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }

        @Test
        fun `skal forskyve tom til siste dag i måneden når tom dato er første dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 1)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.REDUKSJON,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }
    }

    @Nested
    inner class GraderingsforskjellReduksjonTilFullBarnehageplassSammeMånedSomAndreVilkårFørstBlirOppfyltTest {
        @Test
        fun `skal forskyve tom til siste dag i samme måned når tom dato er siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 31)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }

        @Test
        fun `skal forskyve tom til siste dag i forrige måned når tom dato er nest siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 30)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 9, 30))
        }

        @Test
        fun `skal forskyve tom til siste dag i forrige måned når tom dato er første dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 1)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.REDUKSJON_TIL_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 9, 30))
        }
    }

    @Nested
    inner class GraderingsforskjellØkningTest {
        @Test
        fun `skal forskyve tom til siste dag i samme måned når tom er første dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 1)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.ØKNING,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }

        @Test
        fun `skal forskyve tom til siste dag i samme måned når tom er siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 31)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.ØKNING,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }
    }

    @Nested
    inner class ØkningFraFullBarnehageplassSammeMånedSomAndreVilkårFørstBlirOppfyltTest {
        @Test
        fun `skal forskyve tom til siste dag i samme måned når tom dato er siste dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 31)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }

        @Test
        fun `skal forskyve tom til siste dag i samme måned når tom dato er første dag i måneden`() {
            // Arrange
            val tomDato = LocalDate.of(2024, 10, 1)

            // Act
            val forskjøvetTom =
                forskyvTomBasertPåGraderingsforskjell2024(
                    tomDato,
                    Graderingsforskjell.ØKNING_FRA_FULL_BARNEHAGEPLASS_SAMME_MÅNED_SOM_ANDRE_VILKÅR_FØRST_BLIR_OPPFYLT,
                )

            // Assert
            assertThat(forskjøvetTom).isEqualTo(LocalDate.of(2024, 10, 31))
        }
    }
}
