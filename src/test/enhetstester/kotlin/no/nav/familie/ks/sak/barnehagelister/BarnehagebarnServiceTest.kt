package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BarnehagebarnServiceTest {
    private val mockBarnehagebarnRepository = mockk<BarnehagebarnRepository>()

    private val barnehagebarnService =
        BarnehagebarnService(
            mockBarnehagebarnRepository,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
        )

    @Nested
    inner class HentAlleKommuner {
        @Test
        fun `skal hente alle kommuner vi har mottat barnehageliste for`() {
            // Arrange

            val kommuner = setOf("Oslo kommune", "Voss kommune", "Frogn kommune")

            every { mockBarnehagebarnRepository.hentAlleKommuner() } returns kommuner

            // Act
            val alleKommuner = barnehagebarnService.hentAlleKommuner()

            // Assert
            assertThat(alleKommuner).hasSize(3)
            assertThat(alleKommuner).usingRecursiveComparison().isEqualTo(kommuner)
        }
    }
}
