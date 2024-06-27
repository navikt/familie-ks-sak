package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class UtledMaksAntallMånederMedUtbetalingTest {
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
}
