package no.nav.familie.ks.sak.kjerne.avstemming

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingKjøreplan
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class KonsistensavstemmingSchedulerTest {

    @MockK
    private lateinit var envService: EnvService

    @MockK
    private lateinit var konsistensavstemmingKjøreplanService: KonsistensavstemmingKjøreplanService

    @MockK
    private lateinit var taskService: TaskService

    @InjectMockKs
    private lateinit var konsistensavstemmingScheduler: KonsistensavstemmingScheduler

    private val taskSlot = slot<Task>()

    @BeforeEach
    fun setup() {
        every { envService.erLokal() } returns true
        every { taskService.save(capture(taskSlot)) } returns mockk()
    }

    @Test
    fun `utfør skal utføre konsistensavstemming og lager task når dagensdato matchher med kjøreplan`() {
        every { konsistensavstemmingKjøreplanService.plukkLedigKjøreplanFor(any()) } returns
            KonsistensavstemmingKjøreplan(kjøreDato = LocalDate.now())

        konsistensavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.save(any()) }
        val taskData = taskSlot.captured
        assertEquals(KonsistensavstemmingTask.TASK_STEP_TYPE, taskData.type)
    }

    @Test
    fun `utfør skal ikke utføre konsistensavstemming når dagensdato ikke matchher med kjøreplan`() {
        every { konsistensavstemmingKjøreplanService.plukkLedigKjøreplanFor(any()) } returns null

        konsistensavstemmingScheduler.utfør()

        verify(exactly = 0) { taskService.save(any()) }
    }
}
