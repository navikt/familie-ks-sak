package no.nav.familie.ks.sak.integrasjon.distributering

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.integrasjon.distribuering.DistribuerDødsfallBrevPåFagsakTask
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DistribuerDødsfallBrevPåFagsakTaskTest {
    private val brevService = mockk<BrevService>()
    private val distribuerDødsfallBrevPåFagsakTask = DistribuerDødsfallBrevPåFagsakTask(brevService)

    @Test
    fun `doTask skal stoppes dersom task er eldre enn 6 måneder og brev skal ikke bli forsøkt distribuert`() {
        val mockedTask = mockk<Task>()

        every { mockedTask.payload } returns "{\"journalpostId\":\"testId\",\"brevmal\":\"VEDTAK_OPPHØR_DØDSFALL\"}"
        every { mockedTask.opprettetTid } returns LocalDateTime.of(2021, 10, 10, 0, 0)

        distribuerDødsfallBrevPåFagsakTask.doTask(mockedTask)

        verify(exactly = 0) { brevService.prøvDistribuerBrevOgLoggHendelse(any(), any(), any(), any()) }
    }

    @Test
    fun `doTask skal forsøke å distribuere brev dersom task ikke er eldre enn 6mnd`() {
        val mockedTask = mockk<Task>()

        every { mockedTask.payload } returns "{\"journalpostId\":\"testId\",\"brevmal\":\"VEDTAK_OPPHØR_DØDSFALL\"}"
        every { brevService.prøvDistribuerBrevOgLoggHendelse(any(), any(), any(), any()) } just runs
        every { mockedTask.opprettetTid } returns LocalDateTime.now()

        distribuerDødsfallBrevPåFagsakTask.doTask(mockedTask)

        verify(exactly = 1) { brevService.prøvDistribuerBrevOgLoggHendelse(any(), any(), any(), any()) }
    }
}
