package no.nav.familie.ks.sak.integrasjon.journalføring

import FerdigstillOppgaveKnyttJournalpostDto
import NavnOgIdent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import no.nav.familie.ks.sak.api.dto.JournalføringRequestDto
import no.nav.familie.ks.sak.api.dto.NavnOgIdentDto
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.api.dto.TilknyttetBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagMinimalFagsakResponsDto
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringBehandlingstype
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.lagJournalføringRequestDto
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InnkommendeJournalføringServiceTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val opprettBehandlingService: OpprettBehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val loggService: LoggService = mockk()
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val klageService: KlageService = mockk()
    private val innkommendeJournalføringService: InnkommendeJournalføringService =
        InnkommendeJournalføringService(
            integrasjonKlient = integrasjonKlient,
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
            integrasjonKlient.hentTilgangsstyrteJournalposterForBruker(
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
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.journalpostTilgang.harTilgang).isTrue
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
                tilknyttedeBehandlinger =
                    listOf(
                        TilknyttetBehandling(
                            behandlingstype = JournalføringBehandlingstype.FØRSTEGANGSBEHANDLING,
                            behandlingId = gammelBehandling.id.toString(),
                        ),
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
            )

        val oppgaveId = 1L

        val minimalFagsakResponsDto =
            lagMinimalFagsakResponsDto(
                opprettetTidspunkt = fagsak.opprettetTidspunkt,
                id = fagsak.id,
                søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
                status = fagsak.status,
            )

        every { fagsakService.hentEllerOpprettFagsak(any()) } returns minimalFagsakResponsDto
        every { opprettBehandlingService.opprettBehandling(any()) } returns nyBehandling
        every { behandlingService.hentBehandling(gammelBehandling.id) } returns gammelBehandling
        every { integrasjonKlient.hentJournalpost("journalpostId") } returns journalpost
        every { journalføringRepository.save(any()) } returns mockk()
        every { behandlingSøknadsinfoService.lagreNedSøknadsinfo(any(), any()) } just runs
        every { integrasjonKlient.ferdigstillOppgave(any()) } just runs

        // Act
        val faktiskFagsakId = innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        assertThat(faktiskFagsakId).isEqualTo(fagsak.id.toString())
        verify(exactly = 1) { integrasjonKlient.ferdigstillOppgave(any()) }
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
                tilknyttedeBehandlinger =
                    listOf(
                        TilknyttetBehandling(
                            behandlingstype = JournalføringBehandlingstype.FØRSTEGANGSBEHANDLING,
                            behandlingId = gammelBehandling.id.toString(),
                        ),
                    ),
                opprettOgKnyttTilNyBehandling = false,
                datoMottatt = LocalDateTime.now(),
                bruker = NavnOgIdent("Navn navnesen", personIdent),
            )

        val oppgaveId = 1L

        val minimalFagsakResponsDto =
            lagMinimalFagsakResponsDto(
                opprettetTidspunkt = fagsak.opprettetTidspunkt,
                id = fagsak.id,
                søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
                status = fagsak.status,
            )

        every { fagsakService.hentEllerOpprettFagsak(any()) } returns minimalFagsakResponsDto
        every { opprettBehandlingService.opprettBehandling(any()) } returns mockk()
        every { behandlingService.hentBehandling(gammelBehandling.id) } returns gammelBehandling
        every { integrasjonKlient.hentJournalpost("journalpostId") } returns journalpost
        every { journalføringRepository.save(any()) } returns mockk()
        every { behandlingSøknadsinfoService.lagreNedSøknadsinfo(any(), any()) } just runs
        every { integrasjonKlient.ferdigstillOppgave(any()) } just runs

        // Act
        val faktiskFagsakId = innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        assertThat(faktiskFagsakId).isEqualTo(fagsak.id.toString())
        verify(exactly = 1) { integrasjonKlient.ferdigstillOppgave(any()) }
        verify(exactly = 0) { opprettBehandlingService.opprettBehandling(any()) }
        verify(exactly = 1) { journalføringRepository.save(any()) }
    }

    @Test
    fun `knyttJournalpostTilFagsakOgFerdigstillOppgave skal opprette klagebehandling`() {
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
                tilknyttedeBehandlinger = emptyList(),
                opprettOgKnyttTilNyBehandling = true,
                nyBehandlingstype = JournalføringBehandlingstype.KLAGE,
                nyBehandlingsårsak = null,
                datoMottatt = datoMottatt,
                bruker = NavnOgIdent("NavnOgIdent", personIdent),
            )

        val oppgaveId = 1L

        val klageDatoMottattSlot = slot<LocalDate>()

        val minimalFagsakResponsDto =
            lagMinimalFagsakResponsDto(
                opprettetTidspunkt = fagsak.opprettetTidspunkt,
                id = fagsak.id,
                søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
                status = fagsak.status,
            )

        every { fagsakService.hentEllerOpprettFagsak(any()) } returns minimalFagsakResponsDto
        every { integrasjonKlient.hentJournalpost("journalpostId") } returns journalpost
        every { klageService.opprettKlage(fagsak.id, capture(klageDatoMottattSlot)) } returns mockk()
        every { integrasjonKlient.ferdigstillOppgave(any()) } just runs

        // Act
        innkommendeJournalføringService.knyttJournalpostTilFagsakOgFerdigstillOppgave(request = request, oppgaveId = oppgaveId)

        // Assert
        verify(exactly = 1) { klageService.opprettKlage(fagsakId = any(), klageMottattDato = any()) }
        assertThat(klageDatoMottattSlot.captured).isEqualTo(datoMottatt.toLocalDate())
    }

    @Nested
    inner class OppdaterLogiskeVedlegg {
        private val journalpostId = "123"
        private val oppgaveId = "1"
        private val journalpost = lagJournalpost(personIdent = randomFnr(), journalpostId = journalpostId)
        private val fagsak = lagFagsak()

        @BeforeEach
        fun setUp() {
            every { fagsakService.hentEllerOpprettFagsak(any()) } returns
                lagMinimalFagsakResponsDto(
                    opprettetTidspunkt = fagsak.opprettetTidspunkt,
                    id = fagsak.id,
                    søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
                    status = fagsak.status,
                )
            every { integrasjonKlient.hentJournalpost(journalpostId) } returns journalpost
            every { integrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) } just runs
            every { integrasjonKlient.oppdaterJournalpost(any(), any()) } returns OppdaterJournalpostResponse(journalpostId)
            every { integrasjonKlient.ferdigstillJournalpost(any(), any()) } just runs
            every { integrasjonKlient.ferdigstillOppgave(any()) } just runs
            every { klageService.opprettKlage(any<Long>(), any<LocalDate>()) } returns UUID.randomUUID()
        }

        @Test
        fun `skal bulk-oppdatere logiske vedlegg kun for dokumenter der titlene er endret`() {
            // Arrange
            val nyeTitler = listOf("Oppholdstillatelse", "Pass", "Vigselsattest")
            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = nyeTitler.map { LogiskVedlegg(logiskVedleggId = "0", tittel = it) },
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Pass")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verify(exactly = 1) { integrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = nyeTitler)) }
            verify(exactly = 0) { integrasjonKlient.oppdaterLogiskeVedlegg("2", any()) }
        }

        @Test
        fun `skal bulk-oppdatere logiske vedlegg for alle dokumenter der titlene er endret`() {
            // Arrange
            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest")),
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Fødselsattest")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verify(exactly = 1) { integrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = listOf("Vigselsattest"))) }
            verify(exactly = 1) { integrasjonKlient.oppdaterLogiskeVedlegg("2", BulkOppdaterLogiskVedleggRequest(titler = listOf("Fødselsattest"))) }
        }

        @Test
        fun `skal fjerne alle logiske vedlegg når tom liste sendes inn`() {
            // Arrange
            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = emptyList(),
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "123", tittel = "Pass")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verify(exactly = 1) { integrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = emptyList())) }
            verify(exactly = 0) { integrasjonKlient.oppdaterLogiskeVedlegg("2", any()) }
        }

        @Test
        fun `skal ikke oppdatere logiske vedlegg når logiske vedlegg ikke er sendt inn`() {
            // Arrange
            val request = lagJournalføringRequestDtoMedLogiskeVedlegg(logiskeVedleggDokument1 = null, logiskeVedleggDokument2 = null)

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verify(exactly = 0) { integrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) }
        }

        @Test
        fun `skal ikke oppdatere logiske vedlegg når kun logiskVedleggId avviker fra eksisterende`() {
            // Arrange
            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Oppholdstillatelse")),
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Pass")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verify(exactly = 0) { integrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) }
        }

        @Test
        fun `skal bulk-oppdatere logiske vedlegg for dokument som ikke har logiske vedlegg fra før`() {
            // Arrange
            every { integrasjonKlient.hentJournalpost(journalpostId) } returns
                journalpost.copy(dokumenter = journalpost.dokumenter!!.map { it.copy(logiskeVedlegg = null) })

            val titler = (1..50).map { "Vedlegg nummer $it" }
            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = titler.map { LogiskVedlegg(logiskVedleggId = "0", tittel = it) },
                    logiskeVedleggDokument2 = emptyList(),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verify(exactly = 1) { integrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = titler)) }
            verify(exactly = 0) { integrasjonKlient.oppdaterLogiskeVedlegg("2", any()) }
        }

        @Test
        fun `skal ikke endre dokumenttittel ved oppdatering av journalpost`() {
            // Arrange
            val oppdaterJournalpostRequest = slot<OppdaterJournalpostRequestDto>()
            every { integrasjonKlient.oppdaterJournalpost(capture(oppdaterJournalpostRequest), any()) } returns
                OppdaterJournalpostResponse(journalpostId)

            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = (1..50).map { LogiskVedlegg(logiskVedleggId = "0", tittel = "Vedlegg nummer $it") },
                    logiskeVedleggDokument2 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest")),
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            val dokumenter = oppdaterJournalpostRequest.captured.dokumenter!!
            assertThat(dokumenter.map { it.tittel }).containsExactly("Søknad om kontantstøtte", "Ekstra vedlegg")
            assertThat(dokumenter.map { it.logiskeVedlegg }).containsOnlyNulls()
        }

        @Test
        fun `skal oppdatere logiske vedlegg før journalposten oppdateres og ferdigstilles`() {
            // Arrange
            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest")),
                    logiskeVedleggDokument2 = null,
                )

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verifyOrder {
                integrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = listOf("Vigselsattest")))
                integrasjonKlient.oppdaterJournalpost(any(), journalpostId)
                integrasjonKlient.ferdigstillJournalpost(journalpostId, "4820")
                integrasjonKlient.ferdigstillOppgave(oppgaveId.toLong())
            }
        }

        @Test
        fun `skal ikke oppdatere eller ferdigstille journalposten når oppdatering av logiske vedlegg feiler`() {
            // Arrange
            every { integrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) } throws RuntimeException("Dokarkiv er nede")

            val request =
                lagJournalføringRequestDtoMedLogiskeVedlegg(
                    logiskeVedleggDokument1 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest")),
                    logiskeVedleggDokument2 = null,
                )

            // Act & Assert
            val feil = assertThrows<RuntimeException> { innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId) }
            assertThat(feil.message).isEqualTo("Dokarkiv er nede")
            verify(exactly = 0) { integrasjonKlient.oppdaterJournalpost(any(), any()) }
            verify(exactly = 0) { integrasjonKlient.ferdigstillJournalpost(any(), any()) }
            verify(exactly = 0) { integrasjonKlient.ferdigstillOppgave(any()) }
        }

        @Test
        fun `skal oppdatere logiske vedlegg før klagebehandling opprettes`() {
            // Arrange
            val request = lagJournalføringRequestDtoForNyKlage()

            // Act
            innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId)

            // Assert
            verifyOrder {
                integrasjonKlient.oppdaterLogiskeVedlegg("1", BulkOppdaterLogiskVedleggRequest(titler = listOf("Vigselsattest")))
                klageService.opprettKlage(fagsak.id, any<LocalDate>())
                integrasjonKlient.oppdaterJournalpost(any(), journalpostId)
            }
        }

        @Test
        fun `skal ikke opprette klagebehandling når oppdatering av logiske vedlegg feiler`() {
            // Arrange
            every { integrasjonKlient.oppdaterLogiskeVedlegg(any(), any()) } throws RuntimeException("Dokarkiv er nede")

            val request = lagJournalføringRequestDtoForNyKlage()

            // Act & Assert
            val feil = assertThrows<RuntimeException> { innkommendeJournalføringService.journalfør(request, journalpostId, oppgaveId) }
            assertThat(feil.message).isEqualTo("Dokarkiv er nede")
            verify(exactly = 0) { klageService.opprettKlage(any<Long>(), any<LocalDate>()) }
            verify(exactly = 0) { integrasjonKlient.oppdaterJournalpost(any(), any()) }
        }

        private fun lagJournalføringRequestDtoForNyKlage(): JournalføringRequestDto =
            lagJournalføringRequestDtoMedLogiskeVedlegg(
                logiskeVedleggDokument1 = listOf(LogiskVedlegg(logiskVedleggId = "0", tittel = "Vigselsattest")),
                logiskeVedleggDokument2 = null,
            ).copy(
                opprettOgKnyttTilNyBehandling = true,
                nyBehandlingstype = JournalføringBehandlingstype.KLAGE,
            )

        private fun lagJournalføringRequestDtoMedLogiskeVedlegg(
            logiskeVedleggDokument1: List<LogiskVedlegg>?,
            logiskeVedleggDokument2: List<LogiskVedlegg>?,
        ): JournalføringRequestDto {
            val journalføringRequestDto = lagJournalføringRequestDto(bruker = NavnOgIdentDto("Mock", randomFnr()))
            return journalføringRequestDto.copy(
                opprettOgKnyttTilNyBehandling = false,
                dokumenter =
                    listOf(
                        journalføringRequestDto.dokumenter[0].copy(logiskeVedlegg = logiskeVedleggDokument1),
                        journalføringRequestDto.dokumenter[1].copy(logiskeVedlegg = logiskeVedleggDokument2),
                    ),
            )
        }
    }
}
