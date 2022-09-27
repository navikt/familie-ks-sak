package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.vedtak.VedtakService
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import java.time.LocalDate

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

    private val søker = randomAktør()
    private val fagsak = lagFagsak(aktør = søker)
    private val behandling = lagBehandling(
        fagsak,
        opprettetÅrsak = BehandlingÅrsak.SØKNAD
    )

    @BeforeEach
    fun beforeEach() {
        every { personidentService.hentAktør(any()) } returns søker
        every { fagsakRepository.finnFagsakForAktør(søker) } returns fagsak
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns null
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns emptyList()
        every { behandlingRepository.save(any()) } returns behandling
        every { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) } returns Unit
        every { vedtakService.opprettOgInitierNyttVedtakForBehandling(any()) } returns Unit
        every { loggService.opprettBehandlingLogg(any()) } returns Unit
        every { taskRepository.save(any()) } returns OpprettOppgaveTask.opprettTask(
            behandling.id,
            Oppgavetype.BehandleSak,
            LocalDate.now()
        )
    }

    @Test
    fun `opprettBehandling - skal opprette behandling når det finnes en fagsak tilknyttet søker og det ikke allerede eksisterer en aktiv behandling som ikke er avsluttet`() {
        val opprettetBehandling = behandlingService.opprettBehandling(
            OpprettBehandlingDto(
                søkersIdent = "12345678910",
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL
            )
        )
        assertNotNull(opprettetBehandling)
    }

    @Test
    fun `opprettBehandling - skal kaste feil dersom det ikke finnes noen fagsak tilknyttet søker`() {
        every { fagsakRepository.finnFagsakForAktør(søker) } returns null

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = "12345678910",
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL
                )
            )
        }

        assertEquals("Kan ikke lage behandling på person uten tilknyttet fagsak.", funksjonellFeil.melding)
    }

    @Test
    fun `opprettBehandling - skal kaste feil dersom det allerede finnes en aktiv behandling som ikke er avsluttet`() {
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns behandling.copy(
            aktiv = true,
            status = BehandlingStatus.OPPRETTET
        )

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = "12345678910",
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL
                )
            )
        }

        assertEquals(
            "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
            funksjonellFeil.melding
        )
    }
}
