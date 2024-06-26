package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårRegelverkInformasjonForBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtledMaksAntallMånederMedUtbetalingKtTest {
    @Test
    fun `àsdf`() {
        // Arrange
        val vilkårRegelverkInformasjonForBarn =
            VilkårRegelverkInformasjonForBarn(
                LocalDate.of(2022, 10, 1),
            )

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårRegelverkInformasjonForBarn)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(9L)
    }
}
