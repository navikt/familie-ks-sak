package no.nav.familie.ks.sak.kjerne.porteføljejustering

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype.NASJONAL
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.BERGEN
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.DRAMMEN
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.STORD
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.VADSØ
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE

class PorteføljejusteringFlyttOppgaveTaskTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val personidentService: PersonidentService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()

    private val porteføljejusteringFlyttOppgaveTask =
        PorteføljejusteringFlyttOppgaveTask(
            integrasjonKlient = integrasjonKlient,
            behandlingRepository = behandlingRepository,
            personidentService = personidentService,
            fagsakService = fagsakService,
            arbeidsfordelingService = arbeidsfordelingService,
        )

    @Test
    fun `Skal ikke flytte dersom oppgave ikke er tildelt Vadsø eller Bergen`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = STORD.enhetsnummer,
                behandlingstype = NASJONAL.value,
            )

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 0) { integrasjonKlient.hentBehandlendeEnheter(any()) }
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
    }

    @ParameterizedTest
    @EnumSource(value = Behandlingstype::class, names = ["NASJONAL"], mode = EXCLUDE)
    fun `Skal ikke flytte dersom oppgave ikke har behandlingstype NASJONAL`(
        behandlingstype: Behandlingstype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = behandlingstype.toString(),
            )

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 0) { integrasjonKlient.hentBehandlendeEnheter(any()) }
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
    }

    @Test
    fun `Skal kaste feil dersom oppgave ikke er tilknyttet en folkeregistrert ident`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.SAMHANDLERNR)),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Oppgave med id 1 er ikke tilknyttet en ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi ikke får tilbake noen enheter på ident fra norg`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns emptyList()

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Fant ingen arbeidsfordelingsenhet for ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi får tilbake flere enheter på ident fra norg`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BERGEN.enhetsnummer, BERGEN.enhetsnavn),
                Arbeidsfordelingsenhet(DRAMMEN.enhetsnummer, DRAMMEN.enhetsnavn),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Fant flere arbeidsfordelingsenheter for ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi får tilbake Vadsø som enhet på ident fra norg`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns listOf(Arbeidsfordelingsenhet(VADSØ.enhetsnummer, VADSØ.enhetsnavn))

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Oppgave med id 1 tildeles fortsatt Vadsø som enhet")
    }

    @ParameterizedTest
    @EnumSource(value = KontantstøtteEnhet::class, names = ["BERGEN", "VADSØ"], mode = EXCLUDE)
    fun `Skal ikke flytte hvis ny enhet ikke er Bergen`(
        enhet: KontantstøtteEnhet,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns listOf(Arbeidsfordelingsenhet(enhet.enhetsnummer, enhet.enhetsnavn))

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
    }

    @Test
    fun `Skal oppdatere oppgaven med ny enhet og mappe, men ikke behandlingen, dersom saksreferansen ikke er fylt ut`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                mappeId = 100012692,
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns listOf(Arbeidsfordelingsenhet(BERGEN.enhetsnummer, BERGEN.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BERGEN.enhetsnummer, 100012790) } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BERGEN.enhetsnummer, 100012790) }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EXCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, men ikke behandlingen, dersom saksreferanse er fylt ut, men det er ikke av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-ks-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns listOf(Arbeidsfordelingsenhet(BERGEN.enhetsnummer, BERGEN.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BERGEN.enhetsnummer, 100012790) } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BERGEN.enhetsnummer, 100012790) }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven og behandlingen i ks-sak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", 1)
        val aktørPåOppgave = randomAktør()
        val behandling = lagBehandling()

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = VADSØ.enhetsnummer,
                behandlingstype = NASJONAL.value,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-ks-sak",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns listOf(Arbeidsfordelingsenhet(BERGEN.enhetsnummer, BERGEN.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BERGEN.enhetsnummer, 100012790) } returns mockk()
        every { personidentService.hentAktør("1") } returns aktørPåOppgave
        every { fagsakService.hentFagsakForPerson(aktørPåOppgave) } returns lagFagsak()
        every { behandlingRepository.findByFagsakAndAktivAndOpen(any()) } returns behandling
        every { arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, BERGEN.enhetsnummer) } just Runs

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BERGEN.enhetsnummer, 100012790) }
        verify(exactly = 1) { personidentService.hentAktør("1") }
        verify(exactly = 1) { fagsakService.hentFagsakForPerson(aktørPåOppgave) }
        verify(exactly = 1) { behandlingRepository.findByFagsakAndAktivAndOpen(any()) }
        verify(exactly = 1) {
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, BERGEN.enhetsnummer)
        }
    }
}
