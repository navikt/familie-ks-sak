package no.nav.familie.ks.sak.kjerne.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingKjøreplan
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingTaskDto
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KonsistensavstemmingSchedulerTest {
    private val envService = mockk<EnvService>()
    private val konsistensavstemmingKjøreplanService = mockk<KonsistensavstemmingKjøreplanService>()
    private val taskService = mockk<TaskRepositoryWrapper>()

    private val konsistensavstemmingScheduler =
        KonsistensavstemmingScheduler(
            envService = envService,
            konsistensavstemmingKjøreplanService = konsistensavstemmingKjøreplanService,
            taskService = taskService,
        )

    private val taskSlot = slot<Task>()

    @BeforeEach
    fun setup() {
        every { envService.erLokal() } returns true
        every { taskService.save(capture(taskSlot)) } returns mockk()
    }

    @Test
    fun `utfør skal utføre konsistensavstemming og lager task når dagensdato matchher med kjøreplan`() {
        every { konsistensavstemmingKjøreplanService.plukkLedigKjøreplanFor(any()) } returns
            KonsistensavstemmingKjøreplan(kjøredato = LocalDate.now())

        konsistensavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.save(any()) }

        val taskData = taskSlot.captured
        assertEquals(KonsistensavstemmingTask.TASK_STEP_TYPE, taskData.type)
        val konsistensavstemmingTaskDto = objectMapper.readValue(taskData.payload, KonsistensavstemmingTaskDto::class.java)
        assertEquals(0, konsistensavstemmingTaskDto.kjøreplanId)
        assertEquals(LocalDate.now(), konsistensavstemmingTaskDto.initieltKjøreTidspunkt.toLocalDate())
    }

    @Test
    fun `utfør skal ikke utføre konsistensavstemming når dagensdato ikke matchher med kjøreplan`() {
        every { konsistensavstemmingKjøreplanService.plukkLedigKjøreplanFor(any()) } returns null

        konsistensavstemmingScheduler.utfør()

        verify(exactly = 0) { taskService.save(any()) }
    }
}
