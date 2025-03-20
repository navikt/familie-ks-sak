package no.nav.familie.ks.sak.integrasjon.journalføring

import FerdigstillOppgaveKnyttJournalpostDto
import NavnOgIdent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.ks.sak.api.dto.TilknyttetBehandling
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import no.nav.familie.ks.sak.integrasjon.lagTilgangsstyrtJournalpost
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class InnkommendeJournalføringServiceTest {
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val opprettBehandlingService: OpprettBehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val loggService: LoggService = mockk()
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val klageService: KlageService = mockk()
    private val innkommendeJournalføringService: InnkommendeJournalføringService =
        InnkommendeJournalføringService(
            integrasjonClient = integrasjonClient,
            fagsakService = fagsakService,
            opprettBehandlingService = opprettBehandlingService,
            behandlingService = behandlingService,
            journalføringRepository = journalføringRepository,
            loggService = loggService,
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
            klageService = klageService,
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
    fun `knyttJournalpostTilFagsakOgFerdigstillOppgave knytter til både eksisterende og ny behandling`() {
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
                tilknyttedeBehandlinger = listOf(
                    TilknyttetBehandling(
                        behandlingstype = JournalføringBehandlingstype.FØRSTEGANGSBEHANDLING,
                        behandlingId = gammelBehandling.id.toString()
                    )
                ),
                opprettOgKnyttTilNyBehandling = true,
                navIdent = "1234",
                bruker =
                    NavnOgIdent(
                        navn = "Navn navnesen",
                        id = personIdent,
                    ),
                kategori = BehandlingKategori.NASJONAL,
                nyBehandlingstype = JournalføringBehandlingstype.REVURDERING,
                nyBehandlingsårsak = BehandlingÅrsak.SØKNAD,
                datoMottatt = LocalDateTime.now(),
                fagsakId = fagsak.id,
            )

        val oppgaveId = 1L

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
    fun `knyttJournalpostTilFagsakOgFerdigstillOppgave knytter til eksisterende behandling uten å opprette ny behandling`() {
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
                tilknyttedeBehandlinger = listOf(
                    TilknyttetBehandling(
                        behandlingstype = JournalføringBehandlingstype.FØRSTEGANGSBEHANDLING,
                        behandlingId = gammelBehandling.id.toString()
                    )
                ),
                opprettOgKnyttTilNyBehandling = false,
                datoMottatt = LocalDateTime.now(),
                fagsakId = fagsak.id,
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
    fun `knyttJournalpostTilFagsakOgFerdigstillOppgave skal opprette klagebehandling`(){
        // Arrange
        val fagsak = lagFagsak()
        val personIdent = "1234"
        val journalpost =
            lagJournalpost(
                journalpostId = "journalpostId",
                personIdent = personIdent,
                avsenderMottaker = null,
            )

        val datoMottatt = LocalDateTime.now()

        val request =
            FerdigstillOppgaveKnyttJournalpostDto(
                journalpostId = journalpost.journalpostId,
                tilknyttedeBehandlingIder = emptyList(),
                tilknyttedeBehandlinger = emptyList(),
                opprettOgKnyttTilNyBehandling = true,
                nyBehandlingstype = JournalføringBehandlingstype.KLAGE,
                nyBehandlingsårsak = null,
                datoMottatt = datoMottatt,
                fagsakId = fagsak.id,
            )

        val oppgaveId = 1L

        val klageDatoMottattSlot = slot<LocalDate>()
        every { integrasjonClient.hentJournalpost("journalpostId") } returns journalpost
        every { klageService.opprettKlage(fagsak.id, capture(klageDatoMottattSlot)) } returns mockk()
        every { integrasjonClient.ferdigstillOppgave(any()) } just runs

        // Act
        innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        verify(exactly = 1) { klageService.opprettKlage(fagsakId = any(), kravMottattDato = any()) }
        assertThat(klageDatoMottattSlot.captured).isEqualTo(datoMottatt.toLocalDate())
    }

    @Test
    fun `knyttJournalpostTilFagsakOgFerdigstillOppgave skal kaste feil hvis ingen fagsak sendt med og skal ikke knytte til ny behandling`(){
        // Arrange
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
                tilknyttedeBehandlingIder = emptyList(),
                tilknyttedeBehandlinger = emptyList(),
                opprettOgKnyttTilNyBehandling = false,
                datoMottatt = LocalDateTime.now(),
                fagsakId = null,
            )

        val oppgaveId = 1L

        every { integrasjonClient.hentJournalpost("journalpostId") } returns journalpost

        // Act & Assert
        assertThrows<Feil> { innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId) }
    }
}
