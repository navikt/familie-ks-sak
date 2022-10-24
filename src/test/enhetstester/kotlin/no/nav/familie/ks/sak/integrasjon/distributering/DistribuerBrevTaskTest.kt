package no.nav.familie.ks.sak.integrasjon.distributering

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.hentBrevmal
import no.nav.familie.ks.sak.kjerne.brev.hentVedtaksbrevmal
import no.nav.familie.ks.sak.kjerne.brev.hentVedtaksbrevtype
import no.nav.familie.prosessering.domene.Task
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class DistribuerDødsfallBrevPåFagsakTaskTest {
    @MockK
    private lateinit var brevService: BrevService

    @InjectMockKs
    private lateinit var distribuerDødsfallBrevPåFagsakTask: DistribuerDødsfallBrevPåFagsakTask

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
