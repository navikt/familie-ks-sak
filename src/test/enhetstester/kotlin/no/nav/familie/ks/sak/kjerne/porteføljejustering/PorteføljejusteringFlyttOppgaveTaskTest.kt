package no.nav.familie.ks.sak.kjerne.porteføljejustering

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.klage.KlageKlient
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class PorteføljejusteringFlyttOppgaveTaskTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val tilbakekrevingKlient: TilbakekrevingKlient = mockk()
    private val klageKlient: KlageKlient = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val personidentService: PersonidentService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()

    private val porteføljejusteringFlyttOppgaveTask =
        PorteføljejusteringFlyttOppgaveTask(
            integrasjonKlient = integrasjonKlient,
            tilbakekrevingKlient = tilbakekrevingKlient,
            klageKlient = klageKlient,
            behandlingRepository = behandlingRepository,
            personidentService = personidentService,
            fagsakService = fagsakService,
            arbeidsfordelingService = arbeidsfordelingService,
        )

    @Test
    fun `Skal kaste feil dersom oppgave ikke er tilknyttet noe folkeregistrert ident`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.SAMHANDLERNR),
                    ),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }
        assertThat(exception.message).isEqualTo("Oppgave med id 1 er ikke tilknyttet en ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi ikke får tilbake noen enheter på ident ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
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
    fun `Skal kaste feil dersom vi får tilbake flere enn 1 enhet på ident ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.BERGEN.enhetsnummer, KontantstøtteEnhet.BERGEN.enhetsnavn),
                Arbeidsfordelingsenhet(KontantstøtteEnhet.DRAMMEN.enhetsnummer, KontantstøtteEnhet.DRAMMEN.enhetsnavn),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Fant flere arbeidsfordelingsenheter for ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi får tilbake Vadsø som enhet på ident ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.VADSØ.enhetsnummer, KontantstøtteEnhet.VADSØ.enhetsnavn),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Oppgave med id 1 tildeles fortsatt Vadsø som enhet")
    }

    @Test
    fun `Skal stoppe utføringen av task hvis det er midlertidig enhet vi får tilbake ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = KontantstøtteEnhet.VADSØ.enhetsnummer,
                saksreferanse = "referanse",
                oppgavetype = "BEH_SAK",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnummer, KontantstøtteEnhet.MIDLERTIDIG_ENHET.enhetsnavn),
            )

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
        verify { arbeidsfordelingService wasNot Called }
    }

    @Test
    fun `Skal oppdatere oppgaven med ny enhet og mappe og ikke mer dersom saksreferansen ikke er fylt ut`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = KontantstøtteEnhet.VADSØ.enhetsnummer,
                saksreferanse = null,
                oppgavetype = "BEH_SAK",
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-ks-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.BERGEN.enhetsnummer, KontantstøtteEnhet.BERGEN.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.EXCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe og ikke mer dersom saksreferanse er fylt ut, men det er ikke av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = KontantstøtteEnhet.VADSØ.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-ks-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.BERGEN.enhetsnummer, KontantstøtteEnhet.BERGEN.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i ks-sak dersom behandlesAvApplikasjon er familie-ks-sak og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = KontantstøtteEnhet.VADSØ.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-ks-sak",
                aktoerId = "1",
            )
        val aktørPåOppgave = randomAktør()

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.BERGEN.enhetsnummer, KontantstøtteEnhet.BERGEN.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") } returns mockk()

        every { personidentService.hentAktør("1") } returns aktørPåOppgave
        every { fagsakService.hentFagsakForPerson(aktørPåOppgave) } returns lagFagsak()

        val behandling = lagBehandling()
        every { behandlingRepository.findByFagsakAndAktivAndOpen(any()) } returns behandling

        every { arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, KontantstøtteEnhet.BERGEN.enhetsnummer) } just Runs

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") }
        verify(exactly = 1) { personidentService.hentAktør("1") }
        verify(exactly = 1) { fagsakService.hentFagsakForPerson(aktørPåOppgave) }
        verify(exactly = 1) { behandlingRepository.findByFagsakAndAktivAndOpen(any()) }
        verify(exactly = 1) {
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, KontantstøtteEnhet.BERGEN.enhetsnummer)
        }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i klage dersom behandlesAvApplikasjon er familie-klage og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = KontantstøtteEnhet.VADSØ.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-klage",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.BERGEN.enhetsnummer, KontantstøtteEnhet.BERGEN.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") } returns mockk()
        every { klageKlient.oppdaterEnhetPåÅpenBehandling(183421813, "4812") } returns "TODO"

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") }
        verify(exactly = 1) { klageKlient.oppdaterEnhetPåÅpenBehandling(183421813, KontantstøtteEnhet.BERGEN.enhetsnummer) }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i klage dersom behandlesAvApplikasjon er familie-tilbake og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1)

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = KontantstøtteEnhet.VADSØ.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100012692,
                behandlesAvApplikasjon = "familie-tilbake",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnheter("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(KontantstøtteEnhet.BERGEN.enhetsnummer, KontantstøtteEnhet.BERGEN.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") } returns mockk()
        every { tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(183421813, "4812") } returns "TODO"

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, KontantstøtteEnhet.BERGEN.enhetsnummer, "100012790") }
        verify(exactly = 1) { tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(183421813, KontantstøtteEnhet.BERGEN.enhetsnummer) }
    }
}
