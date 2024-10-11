package no.nav.familie.ks.sak.kjerne.regelverk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegelverkUtlederTest {
    @Test
    fun `skal returnere regelverk FØR_LOVENDRING_2025 dersom fødselsdato er før FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = RegelverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.minusDays(1)

        // Act & Assert
        assertThat(RegelverkUtleder.utledRegelverkForBarn(fødselsdato)).isEqualTo(Regelverk.FØR_LOVENDRING_2025)
    }

    @Test
    fun `skal returnere regelverk LOVENDRING_FEBRUAR_2025 dersom fødselsdato er senere enn FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = RegelverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025.plusDays(1)

        // Act & Assert
        assertThat(RegelverkUtleder.utledRegelverkForBarn(fødselsdato)).isEqualTo(Regelverk.LOVENDRING_FEBRUAR_2025)
    }

    @Test
    fun `skal returnere regelverk LOVENDRING_FEBRUAR_2025 dersom fødselsdato FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025`() {
        // Arrange
        val fødselsdato = RegelverkUtleder.FØDSELSDATO_GRENSE_LOVENDRING_FEBRUAR_2025

        // Act & Assert
        assertThat(RegelverkUtleder.utledRegelverkForBarn(fødselsdato)).isEqualTo(Regelverk.LOVENDRING_FEBRUAR_2025)
    }
}
