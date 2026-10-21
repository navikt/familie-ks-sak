package no.nav.familie.ks.sak.barnehagelister

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagebarnRepository
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.data.lagBarnehagebarn
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BarnehagebarnServiceTest {
    private val mockBarnehagebarnRepository = mockk<BarnehagebarnRepository>()
    private val mockTaskService = mockk<TaskRepositoryWrapper>()

    private val barnehagebarnService =
        BarnehagebarnService(
            mockBarnehagebarnRepository,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockTaskService,
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

    @Nested
    inner class LagreBarnehageBarn {
        @Test
        fun `skal opprette task for å populere bydel når barnehagebarnet er i Oslo og har barnehage org nummer`() {
            // Arrange
            val barnehagebarn = lagBarnehagebarn(kommuneNr = "0301", barnehageOrgNummer = "111111111")
            val lagretTask = slot<Task>()
            every { mockBarnehagebarnRepository.saveAndFlush(barnehagebarn) } returns barnehagebarn
            every { mockTaskService.save(capture(lagretTask)) } answers { firstArg() }

            // Act
            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Assert
            assertThat(lagretTask.captured.type).isEqualTo(PopulerBydelForBarnehagebarnTask.TASK_STEP_TYPE)
            assertThat(lagretTask.captured.payload).isEqualTo(barnehagebarn.id.toString())
        }

        @Test
        fun `skal ikke opprette task når barnehagebarnet i Oslo mangler barnehage org nummer`() {
            // Arrange
            val barnehagebarn = lagBarnehagebarn(kommuneNr = "0301", barnehageOrgNummer = null)
            every { mockBarnehagebarnRepository.saveAndFlush(barnehagebarn) } returns barnehagebarn

            // Act
            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Assert
            verify(exactly = 0) { mockTaskService.save(any()) }
        }

        @Test
        fun `skal ikke opprette task når barnehagebarnet er utenfor Oslo`() {
            // Arrange
            val barnehagebarn = lagBarnehagebarn(kommuneNr = "3103", barnehageOrgNummer = "111111111")
            every { mockBarnehagebarnRepository.saveAndFlush(barnehagebarn) } returns barnehagebarn

            // Act
            barnehagebarnService.lagreBarnehageBarn(barnehagebarn)

            // Assert
            verify(exactly = 0) { mockTaskService.save(any()) }
        }
    }
}
