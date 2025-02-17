package no.nav.familie.ks.sak.integrasjon.journalføring

import FerdigstillOppgaveKnyttJournalpostDto
import NavnOgIdent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import no.nav.familie.ks.sak.integrasjon.lagTilgangsstyrtJournalpost
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InnkommendeJournalføringServiceTest {
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val opprettBehandlingService: OpprettBehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val loggService: LoggService = mockk()
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val innkommendeJournalføringService: InnkommendeJournalføringService =
        InnkommendeJournalføringService(
            integrasjonClient = integrasjonClient,
            fagsakService = fagsakService,
            opprettBehandlingService = opprettBehandlingService,
            behandlingService = behandlingService,
            journalføringRepository = journalføringRepository,
            loggService = loggService,
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
        )

    @Test
    fun `skal hente og returnere tilgangsstyrte journalposter`() {
        // Arrange
        val brukerId = "12345678910"
        val journalpostId = "123"
        val tilgangsstyrteJournalposter = listOf(lagTilgangsstyrtJournalpost(personIdent = brukerId, journalpostId = journalpostId, harTilgang = true))

        every {
            integrasjonClient.hentTilgangsstyrteJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                    tema = listOf(Tema.KON),
                ),
            )
        } returns tilgangsstyrteJournalposter

        // Act
        val journalposterForBruker = innkommendeJournalføringService.hentJournalposterForBruker(brukerId)

        // Assert
        assertThat(journalposterForBruker).hasSize(1)
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.harTilgang).isTrue
    }

    @Test
    fun `knyttJournalPostTilFagsakOgFerdigstill Oppgave oppretter ny behandling og knytter til fagsak hvis opprettOgKnyttTilNyBehandling er true`() {
        // Assemble
        val fagsak = lagFagsak()
        val gammelBehandling = lagBehandling(fagsak = fagsak)
        val nyBehandling = lagBehandling(fagsak = fagsak)

        val personIdent = "1234"
        val journalpost =
            lagJournalpost(
                journalpostId = "journalpostId",
                personIdent = personIdent,
                avsenderMottaker = null,
            )

        val request =
            FerdigstillOppgaveKnyttJournalpostDto(
                journalpostId = journalpost.journalpostId,
                tilknyttedeBehandlingIder =
                    listOf(
                        gammelBehandling.behandlingId.id.toString(),
                    ),
                opprettOgKnyttTilNyBehandling = true,
                navIdent = "1234",
                bruker =
                    NavnOgIdent(
                        navn = "Navn navnesen",
                        id = personIdent,
                    ),
                kategori = BehandlingKategori.NASJONAL,
                nyBehandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
                nyBehandlingsårsak = BehandlingÅrsak.SØKNAD,
                datoMottatt = LocalDateTime.now(),
            )

        val oppgaveId = 1L

        every { fagsakService.hentEllerOpprettFagsak(any()) } returns mockk()
        every { opprettBehandlingService.opprettBehandling(any()) } returns nyBehandling
        every { behandlingService.hentBehandling(gammelBehandling.id) } returns gammelBehandling
        every { integrasjonClient.hentJournalpost("journalpostId") } returns journalpost
        every { journalføringRepository.save(any()) } returns mockk()
        every { behandlingSøknadsinfoService.lagreNedSøknadsinfo(any(), any()) } just runs
        every { integrasjonClient.ferdigstillOppgave(any()) } just runs

        // Act
        val faktiskFagsakId = innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        assertThat(faktiskFagsakId).isEqualTo(fagsak.id.toString())
        verify(exactly = 1) { integrasjonClient.ferdigstillOppgave(any()) }
        verify(exactly = 1) { opprettBehandlingService.opprettBehandling(any()) }
        verify(exactly = 2) { journalføringRepository.save(any()) }
    }

    @Test
    fun `knyttJournalPostTilFagsakOgFerdigstill knytter til fagsak uten å opprette ny behandling`() {
        // Assemble
        val fagsak = lagFagsak()
        val gammelBehandling = lagBehandling(fagsak = fagsak)

        val personIdent = "1234"
        val journalpost =
            lagJournalpost(
                journalpostId = "journalpostId",
                personIdent = personIdent,
                avsenderMottaker = null,
            )

        val request =
            FerdigstillOppgaveKnyttJournalpostDto(
                journalpostId = journalpost.journalpostId,
                tilknyttedeBehandlingIder =
                    listOf(
                        gammelBehandling.behandlingId.id.toString(),
                    ),
                opprettOgKnyttTilNyBehandling = false,
                datoMottatt = LocalDateTime.now(),
            )

        val oppgaveId = 1L

        every { fagsakService.hentEllerOpprettFagsak(any()) } returns mockk()
        every { opprettBehandlingService.opprettBehandling(any()) } returns mockk()
        every { behandlingService.hentBehandling(gammelBehandling.id) } returns gammelBehandling
        every { integrasjonClient.hentJournalpost("journalpostId") } returns journalpost
        every { journalføringRepository.save(any()) } returns mockk()
        every { behandlingSøknadsinfoService.lagreNedSøknadsinfo(any(), any()) } just runs
        every { integrasjonClient.ferdigstillOppgave(any()) } just runs

        // Act
        val faktiskFagsakId = innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        assertThat(faktiskFagsakId).isEqualTo(fagsak.id.toString())
        verify(exactly = 1) { integrasjonClient.ferdigstillOppgave(any()) }
        verify(exactly = 0) { opprettBehandlingService.opprettBehandling(any()) }
        verify(exactly = 1) { journalføringRepository.save(any()) }
    }

    @Test
    fun `knyttJournalPostTilFagsakOgFerdigstill returnerer ingen fagsakId hvis det finnes behandlinger på forskjellige fagsaker knyttet til journalposten`() {
        // Assemble
        val fagsak1 = lagFagsak(id = 0)
        val gammelBehandling1 = lagBehandling(fagsak = fagsak1, id = 0)

        val fagsak2 = lagFagsak(id = 1)
        val gammelBehandling2 = lagBehandling(fagsak = fagsak2, id = 1)

        val personIdent = "1234"
        val journalpost =
            lagJournalpost(
                journalpostId = "journalpostId",
                personIdent = personIdent,
                avsenderMottaker = null,
            )

        val request =
            FerdigstillOppgaveKnyttJournalpostDto(
                journalpostId = journalpost.journalpostId,
                tilknyttedeBehandlingIder =
                    listOf(
                        gammelBehandling1.behandlingId.id.toString(),
                        gammelBehandling2.behandlingId.id.toString(),
                    ),
                opprettOgKnyttTilNyBehandling = false,
                datoMottatt = LocalDateTime.now(),
            )

        val oppgaveId = 1L

        every { fagsakService.hentEllerOpprettFagsak(any()) } returns mockk()
        every { opprettBehandlingService.opprettBehandling(any()) } returns mockk()
        every { behandlingService.hentBehandling(gammelBehandling1.id) } returns gammelBehandling1
        every { behandlingService.hentBehandling(gammelBehandling2.id) } returns gammelBehandling2
        every { integrasjonClient.hentJournalpost("journalpostId") } returns journalpost
        every { journalføringRepository.save(any()) } returns mockk()
        every { behandlingSøknadsinfoService.lagreNedSøknadsinfo(any(), any()) } just runs
        every { integrasjonClient.ferdigstillOppgave(any()) } just runs

        // Act
        val faktiskFagsakId = innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        assertThat(faktiskFagsakId).isEqualTo("")
        verify(exactly = 1) { integrasjonClient.ferdigstillOppgave(any()) }
        verify(exactly = 0) { opprettBehandlingService.opprettBehandling(any()) }
        verify(exactly = 2) { journalføringRepository.save(any()) }
    }
}
