package no.nav.familie.ks.sak.kjerne.avstemming

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingKjøreplan
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingTaskDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

internal class KonsistensavstemmingTaskTest {
    private val konsistensavstemmingKjøreplanService = mockk<KonsistensavstemmingKjøreplanService>()
    private val avstemmingService = mockk<AvstemmingService>()
    private val behandlingService = mockk<BehandlingService>()

    private val konsistensavstemmingTask =
        KonsistensavstemmingTask(
            konsistensavstemmingKjøreplanService = konsistensavstemmingKjøreplanService,
            avstemmingService = avstemmingService,
            behandlingService = behandlingService,
        )

    @BeforeEach
    fun setup() {
        every { konsistensavstemmingKjøreplanService.lagreNyStatus(any<Long>(), any()) } just runs
        every { konsistensavstemmingKjøreplanService.harKjøreplanStatusFerdig(1) } returns false
        every { avstemmingService.sendKonsistensavstemmingStartMelding(any(), any()) } just runs
        every { avstemmingService.sendKonsistensavstemmingData(any(), any(), any()) } just runs
        every { avstemmingService.sendKonsistensavstemmingAvsluttMelding(any(), any()) } just runs
    }

    @Test
    fun `doTask skal ikke utføre task når kjøreplan status er FERDIG`() {
        every { konsistensavstemmingKjøreplanService.harKjøreplanStatusFerdig(1) } returns true

        konsistensavstemmingTask.doTask(lagTask())
        verify(exactly = 0) { avstemmingService.sendKonsistensavstemmingStartMelding(any(), any()) }
        verify(exactly = 0) { konsistensavstemmingKjøreplanService.lagreNyStatus(any<KonsistensavstemmingKjøreplan>(), any()) }
    }

    @Test
    fun `doTask skal utføre task for 25000 løpende behandlinger`() {
        val behandlingIder = (1..4999).map { it.toLong() }

        val page = mockk<Page<Long>>()
        val pageable = mockk<Pageable>()
        val nrOfPages = 5
        every { page.totalPages } returns nrOfPages
        every { page.nextPageable() } returns pageable
        every { page.content } returns behandlingIder

        every { behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(any()) } returns page
        every { avstemmingService.hentDataForKonsistensavstemming(any(), any()) } returns mockk()

        konsistensavstemmingTask.doTask(lagTask())

        verify(exactly = 1) { avstemmingService.sendKonsistensavstemmingStartMelding(any(), any()) }
        verify(exactly = 50) { avstemmingService.sendKonsistensavstemmingData(any(), any(), any()) }
        verify(exactly = 1) { avstemmingService.sendKonsistensavstemmingAvsluttMelding(any(), any()) }
        verify(exactly = 1) { konsistensavstemmingKjøreplanService.lagreNyStatus(any<Long>(), any()) }
    }

    private fun lagTask() =
        Task(
            type = KonsistensavstemmingTask.TASK_STEP_TYPE,
            payload =
                jsonMapper.writeValueAsString(
                    KonsistensavstemmingTaskDto(
                        kjøreplanId = 1,
                        initieltKjøreTidspunkt = LocalDateTime.now(),
                    ),
                ),
        )
}
