package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.HenleggÅrsak
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.FeatureToggleConfig
import no.nav.familie.ks.sak.config.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class HenleggBehandlingServiceTest {

    @MockK
    private lateinit var stegService: StegService

    @MockK
    private lateinit var featureToggleService: FeatureToggleService

    @MockK
    private lateinit var brevService: BrevService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var sakStatistikkService: SakStatistikkService

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @InjectMockKs
    private lateinit var henleggBehandlingService: HenleggBehandlingService

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        every { behandlingRepository.hentBehandling(behandlingId) } returns behandling
        every { featureToggleService.isNotEnabled(any()) } returns false
        every { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling) } returns emptyList()
        every { loggService.opprettHenleggBehandlingLogg(any(), any(), any()) } just runs
        every { behandlingRepository.finnBehandlinger(behandling.fagsak.id) } returns listOf(behandling)
        every { fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET) } just runs
        every { stegService.settAlleStegTilAvbrutt(behandling) } just runs
        every { brevService.genererOgSendBrev(any(), any()) } just runs
        every { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) } just runs
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling når den allerede er avsluttet`() {
        every { behandlingRepository.hentBehandling(behandlingId) } returns
            behandling.copy(status = BehandlingStatus.AVSLUTTET)

        val exception = assertThrows<Feil> {
            henleggBehandlingService.henleggBehandling(
                behandlingId = behandlingId,
                henleggÅrsak = HenleggÅrsak.SØKNAD_TRUKKET,
                begrunnelse = ""
            )
        }

        assertEquals(
            "Behandling $behandlingId er allerede avsluttet. Kan ikke henlegge behandling.",
            exception.message
        )
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling for årsak TEKNISK_VEDLIKEHOLD når toggelen er ikke på`() {
        every { featureToggleService.isNotEnabled(FeatureToggleConfig.TEKNISK_VEDLIKEHOLD_HENLEGGELSE) } returns true

        val exception = assertThrows<Feil> {
            henleggBehandlingService.henleggBehandling(
                behandlingId = behandlingId,
                henleggÅrsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                begrunnelse = ""
            )
        }

        assertEquals(
            "Teknisk vedlikehold henleggele er ikke påslått for " +
                "${SikkerhetContext.hentSaksbehandlerNavn()}. Kan ikke henlegge behandling $behandlingId.",
            exception.message
        )
    }

    @Test
    fun `henleggBehandling skal ikke henlegge TEKNISK_ENDRING behandling når toggelen er ikke på`() {
        val tekniskEndringBehandling = behandling.copy(opprettetÅrsak = BehandlingÅrsak.TEKNISK_ENDRING)

        every { behandlingRepository.hentBehandling(behandlingId) } returns tekniskEndringBehandling
        every { featureToggleService.isNotEnabled(FeatureToggleConfig.TEKNISK_ENDRING) } returns true

        val exception = assertThrows<FunksjonellFeil> {
            henleggBehandlingService.henleggBehandling(
                behandlingId = tekniskEndringBehandling.id,
                henleggÅrsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                begrunnelse = ""
            )
        }

        assertEquals(
            "Du har ikke tilgang til å henlegge en behandling " +
                "som er opprettet med årsak=${tekniskEndringBehandling.opprettetÅrsak.visningsnavn}. " +
                "Ta kontakt med teamet dersom dette ikke stemmer.",
            exception.message
        )
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling for årsak  FEILAKTIG_OPPRETTET når behandling er i IVERKSETT_MOT_OPPDRAG steg`() {
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandlingSteg = BehandlingSteg.IVERKSETT_MOT_OPPDRAG,
                behandling = behandling
            )
        )

        every { behandlingRepository.hentBehandling(behandlingId) } returns behandling

        val exception = assertThrows<FunksjonellFeil> {
            henleggBehandlingService.henleggBehandling(
                behandlingId = behandlingId,
                henleggÅrsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                begrunnelse = ""
            )
        }

        assertEquals(
            "Behandling $behandlingId er på steg ${behandling.steg.visningsnavn()} " +
                "og er da låst for alle andre type endringer. Kan ikke henlegge behandling.",
            exception.message
        )
    }

    @Test
    fun `henleggBehandling skal henlegge behandling for årsak TEKNISK_VEDLIKEHOLD når behandling er i IVERKSETT_MOT_OPPDRAG steg`() {
        behandling.behandlingStegTilstand.add(
            BehandlingStegTilstand(
                behandlingSteg = BehandlingSteg.IVERKSETT_MOT_OPPDRAG,
                behandling = behandling
            )
        )

        every { behandlingRepository.hentBehandling(behandlingId) } returns behandling

        assertDoesNotThrow {
            henleggBehandlingService.henleggBehandling(
                behandlingId = behandlingId,
                henleggÅrsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
                begrunnelse = ""
            )
        }
        verify(exactly = 1) { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling) }
        verify(exactly = 1) {
            loggService.opprettHenleggBehandlingLogg(
                behandling,
                HenleggÅrsak.TEKNISK_VEDLIKEHOLD.beskrivelse,
                ""
            )
        }
        verify(exactly = 0) { brevService.sendBrev(any(), any()) }
        verify(exactly = 1) { fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET) }
        verify(exactly = 1) { stegService.settAlleStegTilAvbrutt(behandling) }
        verify(exactly = 1) { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }

        assertEquals(BehandlingStatus.AVSLUTTET, behandling.status)
        assertFalse(behandling.aktiv)
        assertEquals(Behandlingsresultat.HENLAGT_TEKNISK_VEDLIKEHOLD, behandling.resultat)
    }

    @Test
    fun `henleggBehandling skal henlegge behandling for årsak SØKNAD_TRUKKET og sende brev`() {
        assertDoesNotThrow {
            henleggBehandlingService.henleggBehandling(
                behandlingId = behandlingId,
                henleggÅrsak = HenleggÅrsak.SØKNAD_TRUKKET,
                begrunnelse = ""
            )
        }
        verify(exactly = 1) { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling) }
        verify(exactly = 1) {
            loggService.opprettHenleggBehandlingLogg(
                behandling,
                HenleggÅrsak.SØKNAD_TRUKKET.beskrivelse,
                ""
            )
        }
        verify(exactly = 1) { brevService.genererOgSendBrev(any(), any()) }
        verify(exactly = 1) { fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET) }
        verify(exactly = 1) { stegService.settAlleStegTilAvbrutt(behandling) }
        verify(exactly = 1) { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }

        assertEquals(BehandlingStatus.AVSLUTTET, behandling.status)
        assertFalse(behandling.aktiv)
        assertEquals(Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET, behandling.resultat)
    }

    @Test
    fun `henleggBehandling skal henlegge behandling og aktivere siste vedtatt behandling når fagsak har flere behandlinger`() {
        val sisteVedtattBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).also {
            it.aktiv = false
            it.status = BehandlingStatus.AVSLUTTET
            it.resultat = Behandlingsresultat.INNVILGET
        }
        val sisteVedtattBehandlingSlot = slot<Behandling>()
        every { behandlingRepository.finnBehandlinger(behandling.fagsak.id) } returns listOf(sisteVedtattBehandling, behandling)
        every { behandlingRepository.saveAndFlush(capture(sisteVedtattBehandlingSlot)) } returns mockk()

        assertDoesNotThrow {
            henleggBehandlingService.henleggBehandling(
                behandlingId = behandlingId,
                henleggÅrsak = HenleggÅrsak.SØKNAD_TRUKKET,
                begrunnelse = ""
            )
        }
        verify(exactly = 1) { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling) }
        verify(exactly = 1) {
            loggService.opprettHenleggBehandlingLogg(
                behandling,
                HenleggÅrsak.SØKNAD_TRUKKET.beskrivelse,
                ""
            )
        }
        verify(exactly = 1) { brevService.genererOgSendBrev(any(), any()) }
        verify(exactly = 0) { fagsakService.oppdaterStatus(behandling.fagsak, FagsakStatus.AVSLUTTET) }
        verify(exactly = 1) { stegService.settAlleStegTilAvbrutt(behandling) }
        verify(exactly = 1) { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
        verify(exactly = 1) { behandlingRepository.saveAndFlush(any()) }

        assertEquals(BehandlingStatus.AVSLUTTET, behandling.status)
        assertFalse(behandling.aktiv)
        assertEquals(Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET, behandling.resultat)
        assertTrue { sisteVedtattBehandlingSlot.captured.aktiv }
    }
}
