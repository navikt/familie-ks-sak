package no.nav.familie.ks.sak.kjerne.avstemming

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.common.EnvService
import no.nav.familie.ks.sak.common.util.erHelligdag
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.GrensesnittavstemmingTaskDto
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class GrensesnittavstemmingSchedulerTest {
    private val taskService = mockk<TaskRepositoryWrapper>()
    private val envService = mockk<EnvService>()

    private val grensesnittavstemmingScheduler = GrensesnittavstemmingScheduler(taskService, envService)

    @BeforeEach
    fun setup() {
        mockkStatic(LeaderClient::class)
        mockkStatic(LocalDate::erHelligdag)

        every { LocalDate.now().erHelligdag() } returns false
        every { LeaderClient.isLeader() } returns true
        every { envService.erLokal() } returns true
    }

    @Test
    fun `utfør skal utføre scheduler når det ikke finnes ferdige tasker`() {
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns emptyList()
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        assertEquals(LocalDate.now().minusDays(1).atStartOfDay(), taskData.fom)
        assertEquals(LocalDate.now().atStartOfDay(), taskData.tom)
    }

    @Test
    fun `utfør skal utføre scheduler når det finnes siste ferdig task`() {
        // siste kjørte task er på tirsdag for fom mandag 28.11.2022 00:00 og tom tirsdag 29.11.2022 00:00
        val fom = LocalDate.of(2022, 12, 1) // torsdag
        val tom = fom.plusDays(1) // tirsdag
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns listOf(lagTask(fom, tom))
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        assertEquals(LocalDate.of(2022, 12, 2).atStartOfDay(), taskData.fom) // fredag
        assertEquals(LocalDate.of(2022, 12, 5).atStartOfDay(), taskData.tom) // mandag
    }

    @Test
    fun `utfør skal utføre scheduler når scheduler kjører på mandag`() {
        // siste kjørte task er på fredag for fom torsdag 24.11.2022 00:00 og tom fredag 25.11.2022 00:00
        val fom = LocalDate.of(2022, 11, 24)
        val tom = fom.plusDays(1)
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns listOf(lagTask(fom, tom))
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        // siden 26.11.2022, 27.11.2022 er helg
        assertEquals(LocalDate.of(2022, 11, 25).atStartOfDay(), taskData.fom)
        assertEquals(LocalDate.of(2022, 11, 28).atStartOfDay(), taskData.tom)
    }

    @Test
    fun `utfør skal ikke utføre scheduler dersom det er helligdag`() {
        every { LocalDate.now().erHelligdag() } returns true
        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 0) { taskService.finnTasksMedStatus(any(), any(), any()) }
    }

    @Test
    fun `utfør skal utføre scheduler når scheduler kjører på siste dag i desember, tom blir 2 januar`() {
        // siste kjørte task er for fom 30.12.2022 00:00 og tom 31.12.2022 00:00
        val fom = LocalDate.of(2022, 12, 30)
        val tom = fom.plusDays(1)
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns listOf(lagTask(fom, tom))
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        // siden 1. Januar er en heligdag
        assertEquals(LocalDate.of(2022, 12, 31).atStartOfDay(), taskData.fom)
        assertEquals(LocalDate.of(2023, 1, 2).atStartOfDay(), taskData.tom)
    }

    @Test
    fun `utfør skal utføre scheduler når scheduler kjører på siste dag i april, tom blir 2 mai`() {
        // siste kjørte task er for fom 29.04.2022 00:00 og tom 30.04.2022 00:00
        val fom = LocalDate.of(2022, 4, 29)
        val tom = fom.plusDays(1)
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns listOf(lagTask(fom, tom))
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        // siden 1. Mai er en heligdag
        assertEquals(LocalDate.of(2022, 4, 30).atStartOfDay(), taskData.fom)
        assertEquals(LocalDate.of(2022, 5, 2).atStartOfDay(), taskData.tom)
    }

    @Test
    fun `utfør skal utføre scheduler når scheduler kjører på 16 mai, tom blir 18 mai`() {
        // siste kjørte task er for fom 15.05.2022 00:00 og tom 16.05.2022 00:00
        val fom = LocalDate.of(2022, 5, 15)
        val tom = fom.plusDays(1)
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns listOf(lagTask(fom, tom))
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        // siden 17. Mai er en heligdag
        assertEquals(LocalDate.of(2022, 5, 16).atStartOfDay(), taskData.fom)
        assertEquals(LocalDate.of(2022, 5, 18).atStartOfDay(), taskData.tom)
    }

    @Test
    fun `utfør skal utføre scheduler når scheduler kjører på 24 desember, tom blir 27 desember`() {
        // siste kjørte task er for fom 23.12.2022 00:00 og tom 24.12.2022 00:00
        val fom = LocalDate.of(2022, 12, 23)
        val tom = fom.plusDays(1)
        every { taskService.finnTasksMedStatus(any(), any(), any()) } returns listOf(lagTask(fom, tom))
        val taskDataSlot = slot<Task>()
        every { taskService.save(capture(taskDataSlot)) } returns mockk()

        grensesnittavstemmingScheduler.utfør()

        verify(exactly = 1) { taskService.finnTasksMedStatus(any(), any(), any()) }
        verify(exactly = 1) { taskService.save(any()) }

        val taskData = jsonMapper.readValue(taskDataSlot.captured.payload, GrensesnittavstemmingTaskDto::class.java)
        // siden 25. desember og 26.desember er heligdager
        assertEquals(LocalDate.of(2022, 12, 24).atStartOfDay(), taskData.fom)
        assertEquals(LocalDate.of(2022, 12, 27).atStartOfDay(), taskData.tom)
    }

    private fun lagTask(
        fom: LocalDate,
        tom: LocalDate,
    ) = Task(
        type = GrensesnittavstemmingTask.TASK_STEP_TYPE,
        payload = jsonMapper.writeValueAsString(GrensesnittavstemmingTaskDto(fom.atStartOfDay(), tom.atStartOfDay(), UUID.randomUUID())),
    )
}
