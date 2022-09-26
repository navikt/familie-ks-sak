package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.vedtak.VedtakService
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingServiceTest {
    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var taskRepository: TaskRepository

    @InjectMockKs
    private lateinit var behandlingService: BehandlingService

    @Test
    fun `opprettBehandling - skal opprette behandling når det finnes en fagsak tilknyttet søker og det ikke allerede eksisterer en aktiv behandling som ikke er avsluttet`() {
        every { personidentService.hentAktør(any()) } returns 

        val behandling = behandlingService.opprettBehandling(
            OpprettBehandlingDto(
                søkersIdent = "12345678910",
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
            )
        )
        assertNotNull(behandling)
    }
}
