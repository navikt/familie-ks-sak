package no.nav.familie.ks.sak.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PreprodControllerTest {
    @Test
    fun `skal returnere deployet branch og versjon`() {
        // Arrange
        val preprodController = PreprodController(branch = "NAV-30081_min_branch", versjon = "familie-ks-sak:abc123")

        // Act
        val respons = preprodController.hentVersjonsinfo()

        // Assert
        assertThat(respons.body?.data?.branch).isEqualTo("NAV-30081_min_branch")
        assertThat(respons.body?.data?.versjon).isEqualTo("familie-ks-sak:abc123")
    }
}
