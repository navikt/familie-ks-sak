package no.nav.familie.ks.sak.integrasjon.oppgave

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.DbOppgave
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.OppgaveRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.tilArbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveServiceTest {
    private val mockedIntegrasjonClient: IntegrasjonClient = mockk()
    private val mockedOppgaveRepository: OppgaveRepository = mockk()
    private val mockedBehandlingRepository: BehandlingRepository = mockk()
    private val mockedTilpassArbeidsfordelingService: TilpassArbeidsfordelingService = mockk()
    private val mockedArbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val oppgaveService: OppgaveService =
        OppgaveService(
            integrasjonClient = mockedIntegrasjonClient,
            oppgaveRepository = mockedOppgaveRepository,
            behandlingRepository = mockedBehandlingRepository,
            tilpassArbeidsfordelingService = mockedTilpassArbeidsfordelingService,
            arbeidsfordelingPåBehandlingRepository = mockedArbeidsfordelingPåBehandlingRepository,
        )

    @Nested
    inner class OpprettOppgaveTest {
        @Test
        fun `skal opprette oppgave med enhet fra arbeidsfordeling da NAV-ident har tilgang til enheten`() {
            // Arrange
            val navIdent = NavIdent("123")
            val behandlingId = 1L
            val oppgavetype = Oppgavetype.BehandleSak
            val oppgaveId: Long = 1
            val fristForFerdigstillelse = LocalDate.now().plusYears(1)

            val fagsak =
                lagFagsak(
                    id = 0,
                )

            val behandling =
                lagBehandling(
                    id = behandlingId,
                    fagsak = fagsak,
                    kategori = BehandlingKategori.NASJONAL,
                )

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.DRAMMEN.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.DRAMMEN.enhetsnavn,
                    manueltOverstyrt = true,
                )

            val arbeidsfordelingsenhet = arbeidsfordelingPåBehandling.tilArbeidsfordelingsenhet()

            every {
                mockedBehandlingRepository.hentBehandling(behandlingId)
            } returns behandling

            every {
                mockedOppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                    oppgavetype = oppgavetype,
                    behandling = behandling,
                )
            } returns null

            every {
                mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                )
            } returns arbeidsfordelingPåBehandling

            every {
                mockedTilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = navIdent,
                )
            } returns navIdent

            val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()
            every {
                mockedIntegrasjonClient.opprettOppgave(
                    capture(opprettOppgaveRequestSlot),
                )
            } returns OppgaveResponse(oppgaveId = oppgaveId)

            every {
                mockedOppgaveRepository.save(
                    any(),
                )
            } returnsArgument 0

            // Act
            val opprettOppgaveId =
                oppgaveService.opprettOppgave(
                    behandlingId,
                    oppgavetype,
                    fristForFerdigstillelse,
                    navIdent.ident,
                )

            // Assert
            assertThat(opprettOppgaveId).isEqualTo(oppgaveId.toString())
            val capturedOpprettOppgaveRequest = opprettOppgaveRequestSlot.captured
            assertThat(capturedOpprettOppgaveRequest.ident?.ident).isEqualTo(fagsak.aktør.aktørId)
            assertThat(capturedOpprettOppgaveRequest.ident?.gruppe).isEqualTo(IdentGruppe.AKTOERID)
            assertThat(capturedOpprettOppgaveRequest.saksId).isEqualTo(fagsak.id.toString())
            assertThat(capturedOpprettOppgaveRequest.tema).isEqualTo(Tema.KON)
            assertThat(capturedOpprettOppgaveRequest.oppgavetype).isEqualTo(oppgavetype)
            assertThat(capturedOpprettOppgaveRequest.fristFerdigstillelse).isEqualTo(fristForFerdigstillelse)
            assertThat(capturedOpprettOppgaveRequest.beskrivelse).contains("https://kontantstotte.intern.nav.no/fagsak/${fagsak.id}")
            assertThat(capturedOpprettOppgaveRequest.enhetsnummer).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(capturedOpprettOppgaveRequest.behandlingstema).isNull()
            assertThat(capturedOpprettOppgaveRequest.aktivFra).isBeforeOrEqualTo(LocalDate.now())
            assertThat(capturedOpprettOppgaveRequest.behandlesAvApplikasjon).isNull()
            assertThat(capturedOpprettOppgaveRequest.tilordnetRessurs).isEqualTo(navIdent.ident)
            verify(exactly = 1) { mockedOppgaveRepository.save(any()) }
            verify(exactly = 0) { mockedArbeidsfordelingPåBehandlingRepository.save(any()) }
        }

        @Test
        fun `skal opprette oppgave med enhet fra arbeidsfordeling da NAV-ident er null`() {
            // Arrange
            val behandlingId = 1L
            val oppgavetype = Oppgavetype.BehandleSak
            val oppgaveId: Long = 1
            val fristForFerdigstillelse = LocalDate.now().plusYears(1)

            val fagsak =
                lagFagsak(
                    id = 0,
                )

            val behandling =
                lagBehandling(
                    id = behandlingId,
                    fagsak = fagsak,
                    kategori = BehandlingKategori.NASJONAL,
                )

            val arbeidsfordelingPåBehandling =
                lagArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                    behandlendeEnhetId = KontantstøtteEnhet.DRAMMEN.enhetsnummer,
                    behandlendeEnhetNavn = KontantstøtteEnhet.DRAMMEN.enhetsnavn,
                    manueltOverstyrt = true,
                )

            val arbeidsfordelingsenhet = arbeidsfordelingPåBehandling.tilArbeidsfordelingsenhet()

            every {
                mockedBehandlingRepository.hentBehandling(behandlingId)
            } returns behandling

            every {
                mockedOppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                    oppgavetype = oppgavetype,
                    behandling = behandling,
                )
            } returns null

            every {
                mockedArbeidsfordelingPåBehandlingRepository.hentArbeidsfordelingPåBehandling(
                    behandlingId = behandlingId,
                )
            } returns arbeidsfordelingPåBehandling

            every {
                mockedTilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(
                    arbeidsfordelingsenhet = arbeidsfordelingsenhet,
                    navIdent = null,
                )
            } returns null

            val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()
            every {
                mockedIntegrasjonClient.opprettOppgave(
                    capture(opprettOppgaveRequestSlot),
                )
            } returns OppgaveResponse(oppgaveId = oppgaveId)

            every {
                mockedOppgaveRepository.save(
                    any(),
                )
            } returnsArgument 0

            // Act
            val opprettOppgaveId =
                oppgaveService.opprettOppgave(
                    behandlingId,
                    oppgavetype,
                    fristForFerdigstillelse,
                )

            // Assert
            assertThat(opprettOppgaveId).isEqualTo(oppgaveId.toString())
            val capturedOpprettOppgaveRequest = opprettOppgaveRequestSlot.captured
            assertThat(capturedOpprettOppgaveRequest.ident?.ident).isEqualTo(fagsak.aktør.aktørId)
            assertThat(capturedOpprettOppgaveRequest.ident?.gruppe).isEqualTo(IdentGruppe.AKTOERID)
            assertThat(capturedOpprettOppgaveRequest.saksId).isEqualTo(fagsak.id.toString())
            assertThat(capturedOpprettOppgaveRequest.tema).isEqualTo(Tema.KON)
            assertThat(capturedOpprettOppgaveRequest.oppgavetype).isEqualTo(oppgavetype)
            assertThat(capturedOpprettOppgaveRequest.fristFerdigstillelse).isEqualTo(fristForFerdigstillelse)
            assertThat(capturedOpprettOppgaveRequest.beskrivelse).contains("https://kontantstotte.intern.nav.no/fagsak/${fagsak.id}")
            assertThat(capturedOpprettOppgaveRequest.enhetsnummer).isEqualTo(arbeidsfordelingsenhet.enhetId)
            assertThat(capturedOpprettOppgaveRequest.behandlingstema).isNull()
            assertThat(capturedOpprettOppgaveRequest.aktivFra).isBeforeOrEqualTo(LocalDate.now())
            assertThat(capturedOpprettOppgaveRequest.behandlesAvApplikasjon).isNull()
            assertThat(capturedOpprettOppgaveRequest.tilordnetRessurs).isNull()
            verify(exactly = 1) { mockedOppgaveRepository.save(any()) }
            verify(exactly = 0) { mockedArbeidsfordelingPåBehandlingRepository.save(any()) }
        }
    }

    @Nested
    inner class EndreBehandlingstemaPåOppgaverForBehandling {
        @Test
        fun `skal oppdatere behandlingstype for alle oppgaver som ikke er ferdigstilt for behandling`() {
            // Arrange
            val behandling = lagBehandling()

            val dbOppgave1 = mockk<DbOppgave>().apply { every { gsakId } returns "1" }
            val dbOppgave2 = mockk<DbOppgave>().apply { every { gsakId } returns "2" }
            val oppgave1 = Oppgave(id = 1, behandlingstype = "NASJONAL")
            val oppgave2 = Oppgave(id = 2, behandlingstype = "NASJONAL")
            val oppdaterteOppgaveList = mutableListOf<Oppgave>()

            every { mockedOppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling) } returns listOf(dbOppgave1, dbOppgave2)
            every { mockedIntegrasjonClient.finnOppgaveMedId(1) } returns oppgave1
            every { mockedIntegrasjonClient.finnOppgaveMedId(2) } returns oppgave2
            every { mockedIntegrasjonClient.oppdaterOppgave(capture(oppdaterteOppgaveList)) } just runs

            // Act
            oppgaveService.oppdaterBehandlingstypePåOppgaverFraBehandling(behandling)

            // Assert
            verify(exactly = 1) { mockedOppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling) }
            verify(exactly = 1) { mockedIntegrasjonClient.finnOppgaveMedId(1) }
            verify(exactly = 1) { mockedIntegrasjonClient.finnOppgaveMedId(2) }
            verify(exactly = 2) { mockedIntegrasjonClient.oppdaterOppgave(any()) }

            assertThat(oppdaterteOppgaveList.all { it.behandlingstype == "EØS" })
        }
    }
}
