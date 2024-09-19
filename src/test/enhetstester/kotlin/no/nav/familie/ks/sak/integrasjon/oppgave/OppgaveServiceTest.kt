package no.nav.familie.ks.sak.integrasjon.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.OppgaveRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
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
    private val mockedNavIdentOgEnhetService: NavIdentOgEnhetService = mockk()
    private val mockedArbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()
    private val oppgaveService: OppgaveService =
        OppgaveService(
            integrasjonClient = mockedIntegrasjonClient,
            oppgaveRepository = mockedOppgaveRepository,
            behandlingRepository = mockedBehandlingRepository,
            navIdentOgEnhetService = mockedNavIdentOgEnhetService,
            arbeidsfordelingPåBehandlingRepository = mockedArbeidsfordelingPåBehandlingRepository,
        )

    @Nested
    inner class OpprettOppgaveTest {
        @Test
        fun `skal opprette oppgave med annen enhet enn arbeidsfordeling tilsier da NAV-ident ikke har tilgang til enheten fra arbeidsfordeling`() {
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

            val navIdentOgEnhet =
                NavIdentOgEnhet(
                    navIdent = navIdent,
                    enhetsnummer = KontantstøtteEnhet.OSLO.enhetsnummer,
                    enhetsnavn = KontantstøtteEnhet.OSLO.enhetsnavn,
                )

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
                mockedNavIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )
            } returns navIdentOgEnhet

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

            val arbeidsfordelingPåBehandlingSlot = slot<ArbeidsfordelingPåBehandling>()
            every {
                mockedArbeidsfordelingPåBehandlingRepository.save(
                    capture(arbeidsfordelingPåBehandlingSlot),
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
            assertThat(capturedOpprettOppgaveRequest.beskrivelse).contains("https://ks.intern.nav.no/fagsak/${fagsak.id}")
            assertThat(capturedOpprettOppgaveRequest.enhetsnummer).isEqualTo(navIdentOgEnhet.enhetsnummer)
            assertThat(capturedOpprettOppgaveRequest.behandlingstema).isNull()
            assertThat(capturedOpprettOppgaveRequest.aktivFra).isBeforeOrEqualTo(LocalDate.now())
            assertThat(capturedOpprettOppgaveRequest.behandlesAvApplikasjon).isNull()
            assertThat(capturedOpprettOppgaveRequest.tilordnetRessurs).isEqualTo(navIdent.ident)
            val capturedArbeidsfordelingPåBehandling = arbeidsfordelingPåBehandlingSlot.captured
            assertThat(capturedArbeidsfordelingPåBehandling.id).isEqualTo(0)
            assertThat(capturedArbeidsfordelingPåBehandling.behandlingId).isEqualTo(behandlingId)
            assertThat(capturedArbeidsfordelingPåBehandling.behandlendeEnhetId).isEqualTo(navIdentOgEnhet.enhetsnummer)
            assertThat(capturedArbeidsfordelingPåBehandling.behandlendeEnhetNavn).isEqualTo(navIdentOgEnhet.enhetsnavn)
            assertThat(capturedArbeidsfordelingPåBehandling.manueltOverstyrt).isFalse()
            verify(exactly = 1) { mockedOppgaveRepository.save(any()) }
            verify(exactly = 1) { mockedArbeidsfordelingPåBehandlingRepository.save(any()) }
        }

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

            val navIdentOgEnhet =
                NavIdentOgEnhet(
                    navIdent = navIdent,
                    enhetsnummer = arbeidsfordelingPåBehandling.behandlendeEnhetId,
                    enhetsnavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                )

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
                mockedNavIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = navIdent,
                )
            } returns navIdentOgEnhet

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
            assertThat(capturedOpprettOppgaveRequest.beskrivelse).contains("https://ks.intern.nav.no/fagsak/${fagsak.id}")
            assertThat(capturedOpprettOppgaveRequest.enhetsnummer).isEqualTo(navIdentOgEnhet.enhetsnummer)
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

            val navIdentOgEnhet =
                NavIdentOgEnhet(
                    navIdent = null,
                    enhetsnummer = arbeidsfordelingPåBehandling.behandlendeEnhetId,
                    enhetsnavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
                )

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
                mockedNavIdentOgEnhetService.hentNavIdentOgEnhet(
                    arbeidsfordelingPåBehandling = arbeidsfordelingPåBehandling,
                    navIdent = null,
                )
            } returns navIdentOgEnhet

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
            assertThat(capturedOpprettOppgaveRequest.beskrivelse).contains("https://ks.intern.nav.no/fagsak/${fagsak.id}")
            assertThat(capturedOpprettOppgaveRequest.enhetsnummer).isEqualTo(navIdentOgEnhet.enhetsnummer)
            assertThat(capturedOpprettOppgaveRequest.behandlingstema).isNull()
            assertThat(capturedOpprettOppgaveRequest.aktivFra).isBeforeOrEqualTo(LocalDate.now())
            assertThat(capturedOpprettOppgaveRequest.behandlesAvApplikasjon).isNull()
            assertThat(capturedOpprettOppgaveRequest.tilordnetRessurs).isNull()
            verify(exactly = 1) { mockedOppgaveRepository.save(any()) }
            verify(exactly = 0) { mockedArbeidsfordelingPåBehandlingRepository.save(any()) }
        }
    }
}
