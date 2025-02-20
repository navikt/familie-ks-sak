package no.nav.familie.ks.sak.kjerne.lovverk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LovverkUtlederTest {
    @Test
    fun `skal returnere lovverk FØR_LOVENDRING_2025 dersom fødselsdato er før FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusDays(1)

        // Act & Assert
        assertThat(LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = null)).isEqualTo(Lovverk.FØR_LOVENDRING_2025)
    }

    @Test
    fun `skal returnere lovverk LOVENDRING_FEBRUAR_2025 dersom fødselsdato er senere enn FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.plusDays(1)

        // Act & Assert
        assertThat(LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = null)).isEqualTo(Lovverk.LOVENDRING_FEBRUAR_2025)
    }

    @Test
    fun `skal returnere lovverk LOVENDRING_FEBRUAR_2025 dersom fødselsdato er nøyaktig FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025

        // Act & Assert
        assertThat(LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = null)).isEqualTo(Lovverk.LOVENDRING_FEBRUAR_2025)
    }

    @Test
    fun `skal returnere lovverk LOVENDRING_FEBRUAR_2025 dersom adopsjonsdato er nøyaktig FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025, selv om fødselsdatoen er tidligere`() {
        // Arrange
        val fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusDays(1)
        val adopsjonsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025

        // Act & Assert
        assertThat(LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = adopsjonsdato)).isEqualTo(Lovverk.LOVENDRING_FEBRUAR_2025)
    }

    @Test
    fun `skal returnere lovverk LOVENDRING_FEBRUAR_2025 dersom adopsjonsdato er senere enn FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025, selv om fødselsdatoen er tidligere`() {
        // Arrange
        val fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusDays(1)
        val adopsjonsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.plusDays(1)

        // Act & Assert
        assertThat(LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = adopsjonsdato)).isEqualTo(Lovverk.LOVENDRING_FEBRUAR_2025)
    }

    @Test
    fun `skal returnere lovverk FØR_LOVENDRING_2025 dersom adopsjonsdato er tidligere enn FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusMonths(1)
        val adopsjonsdato = LovverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusDays(1)

        // Act & Assert
        assertThat(LovverkUtleder.utledLovverkForBarn(fødselsdato = fødselsdato, adopsjonsdato = adopsjonsdato)).isEqualTo(Lovverk.FØR_LOVENDRING_2025)
    }
}
