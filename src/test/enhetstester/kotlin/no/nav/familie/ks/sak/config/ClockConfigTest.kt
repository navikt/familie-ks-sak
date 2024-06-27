package no.nav.familie.ks.sak.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClockConfigTest {
    @Test
    fun `Skal opprette en clock`() {
        // Arrange
        val clockConfig = ClockConfig()

        // Act
        val clock = clockConfig.clock()

        // Assert
        assertThat(clock).isNotNull()
    }
}
