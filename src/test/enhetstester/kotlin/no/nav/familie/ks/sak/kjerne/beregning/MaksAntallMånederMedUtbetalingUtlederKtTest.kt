package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MaksAntallMånederMedUtbetalingUtlederKtTest {
    @ParameterizedTest
    @CsvSource("5, 11", "6, 11", "7, 11", "8, 11")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 11 måneder for barn født før september 2022`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val vilkårRegelverkInformasjonForBarn =
            VilkårRegelverkInformasjonForBarn(
                LocalDate.of(2022, måned, 1),
            )

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @ParameterizedTest
    @CsvSource("9, 10", "10, 9", "11, 8", "12, 7")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 7-10 måneder for barn født sept-des 2022`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val vilkårRegelverkInformasjonForBarn =
            VilkårRegelverkInformasjonForBarn(
                LocalDate.of(2022, måned, 1),
            )

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @ParameterizedTest
    @CsvSource("1, 7", "2, 7", "3, 7", "4, 7")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 7 måneder dersom barn er født i januar 2023 eller senere`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val vilkårRegelverkInformasjonForBarn =
            VilkårRegelverkInformasjonForBarn(
                LocalDate.of(2023, måned, 1),
            )

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @ParameterizedTest
    @CsvSource("8, 7", "9, 7", "10, 7", "11, 7", "12, 7")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 7 måneder for barn kun truffet av 2024 regelverk`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val vilkårRegelverkInformasjonForBarn =
            VilkårRegelverkInformasjonForBarn(
                LocalDate.of(2024, måned, 1),
            )

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @Test
    fun `utledMaksAntallMånederMedUtbetaling - skal kaste feil om barnet hverken er påvirket av 2021 eller 2024 regelverk `() {
        // Arrange
        val vilkårRegelverkInformasjonForBarn: VilkårRegelverkInformasjonForBarn = mockk()

        every { vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2021 } returns false
        every { vilkårRegelverkInformasjonForBarn.erTruffetAvRegelverk2024 } returns false

        // Act & assert
        val exception =
            assertThrows<Feil> {
                utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn)
            }
        assertThat(exception.message).isEqualTo(
            "Barnets vilkår blir verken truffet av 2021 eller 2024 regelverket. Dette skal ikke være mulig.",
        )
    }
}
