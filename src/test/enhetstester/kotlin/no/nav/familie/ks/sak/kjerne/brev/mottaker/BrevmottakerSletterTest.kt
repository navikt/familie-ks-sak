package no.nav.familie.ks.sak.kjerne.brev.mottaker

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.datagenerator.lagBrevmottakerDb
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

class BrevmottakerSletterTest {
    private val brevmottakerRepository = mockk<BrevmottakerRepository>()
    private val loggService = mockk<LoggService>()
    private val brevmottakerSletter =
        BrevmottakerSletter(
            brevmottakerRepository = brevmottakerRepository,
            loggService = loggService,
        )

    @Test
    fun `skal logge og slette brevmottaker`() {
        // Arrange
        val brevmottaker = lagBrevmottakerDb(id = 200, behandlingId = 1)

        every { brevmottakerRepository.findByIdOrNull(200) } returns brevmottaker
        every { loggService.opprettBrevmottakerLogg(brevmottaker, true) } just runs
        every { brevmottakerRepository.deleteById(200) } just runs

        // Act
        brevmottakerSletter.slettBrevmottaker(behandlingId = 1, id = 200)

        // Assert
        verify(exactly = 1) { loggService.opprettBrevmottakerLogg(brevmottaker, true) }
        verify(exactly = 1) { brevmottakerRepository.deleteById(200) }
    }

    @Test
    fun `skal verken logge eller slette når brevmottakeren ikke finnes`() {
        // Arrange
        every { brevmottakerRepository.findByIdOrNull(404) } returns null

        // Act & Assert
        assertThatThrownBy { brevmottakerSletter.slettBrevmottaker(behandlingId = 1, id = 404) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Finner ikke brevmottaker med id=404 på behandling med id=1")

        verify(exactly = 0) { loggService.opprettBrevmottakerLogg(any(), any()) }
        verify(exactly = 0) { brevmottakerRepository.deleteById(any()) }
    }

    @Test
    fun `skal verken logge eller slette når brevmottakeren tilhører en annen behandling`() {
        // Arrange
        val brevmottaker = lagBrevmottakerDb(id = 200, behandlingId = 2)
        every { brevmottakerRepository.findByIdOrNull(200) } returns brevmottaker

        // Act & Assert
        assertThatThrownBy { brevmottakerSletter.slettBrevmottaker(behandlingId = 1, id = 200) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Finner ikke brevmottaker med id=200 på behandling med id=1")

        verify(exactly = 0) { loggService.opprettBrevmottakerLogg(any(), any()) }
        verify(exactly = 0) { brevmottakerRepository.deleteById(any()) }
    }
}
