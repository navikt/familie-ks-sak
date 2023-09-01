package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class StegServiceUnitTest {

    @MockK
    private lateinit var steg: List<IBehandlingSteg>

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @MockK
    private lateinit var sakStatistikkService: SakStatistikkService

    @MockK
    private lateinit var taskService: TaskService

    @MockK
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var stegService: StegService

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `settBehandlingstegTilstandPåVent - skal sette årsak frist og status på behandlingstegtilstand til nåværende behandlingssteg`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        val årsak = VenteÅrsak.AVVENTER_DOKUMENTASJON

        stegService.settBehandlingstegPåVent(
            behandling,
            frist,
            årsak,
        )

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.VENTER, behandlingStegTilstand.behandlingStegStatus)
        assertEquals(frist, behandlingStegTilstand.frist)
        assertEquals(VenteÅrsak.AVVENTER_DOKUMENTASJON, behandlingStegTilstand.årsak)
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal oppdatere årsak og eller frist på behandlingstegtilstand til nåværende behandlingssteg og returnere gamle verdier for frist og årsak`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
        every { loggService.opprettOppdaterVentingLogg(any(), any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        val årsak = VenteÅrsak.AVVENTER_DOKUMENTASJON

        stegService.settBehandlingstegPåVent(
            behandling,
            frist,
            årsak,
        )

        val nyFrist = LocalDate.now().plusMonths(1)

        val gammelFrist = stegService.oppdaterBehandlingstegFristOgÅrsak(
            behandling,
            nyFrist,
            årsak,
        )

        val behandling = behandlingSlot.captured
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        assertEquals(BehandlingStegStatus.VENTER, behandlingStegTilstand.behandlingStegStatus)
        assertEquals(nyFrist, behandlingStegTilstand.frist)
        assertEquals(VenteÅrsak.AVVENTER_DOKUMENTASJON, behandlingStegTilstand.årsak)

        assertEquals(frist, gammelFrist)
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal ikke oppdatere årsak og eller frist på behandlingstegtilstand når frist og årsak er uendret`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
        every { loggService.opprettOppdaterVentingLogg(any(), any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        val årsak = VenteÅrsak.AVVENTER_DOKUMENTASJON

        stegService.settBehandlingstegPåVent(
            behandling,
            frist,
            årsak,
        )

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            stegService.oppdaterBehandlingstegFristOgÅrsak(
                behandling,
                frist,
                årsak,
            )
        }

        assertEquals(
            "Behandlingen er allerede satt på vent med frist $frist og årsak $årsak.",
            funksjonellFeil.message,
        )

        verify(exactly = 1) { behandlingRepository.saveAndFlush(any()) }
    }
}
