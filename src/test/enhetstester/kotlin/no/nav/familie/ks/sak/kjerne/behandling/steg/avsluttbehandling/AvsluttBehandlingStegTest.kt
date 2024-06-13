package no.nav.familie.ks.sak.kjerne.behandling.steg.avsluttbehandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingMetrikker
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class AvsluttBehandlingStegTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingMetrikker: BehandlingMetrikker

    @MockK
    private lateinit var snikeIKøenService: SnikeIKøenService

    @InjectMockKs
    private lateinit var avsluttBehandlingSteg: AvsluttBehandlingSteg

    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        behandling =
            lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
                .also { it.status = BehandlingStatus.IVERKSETTER_VEDTAK }

        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { loggService.opprettAvsluttBehandlingLogg(behandling) } just runs
        every { behandlingMetrikker.oppdaterBehandlingMetrikker(behandling) } just runs
    }

    @Test
    fun `utførSteg skal ikke utføre steg når behandling ikke har IVERKSETT_VEDTAK status`() {
        behandling.status = BehandlingStatus.UTREDES

        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        val exception = assertThrows<Feil> { avsluttBehandlingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Prøver å ferdigstille behandling ${behandling.id}, men status er ${behandling.status}",
            exception.message,
        )
    }

    @Test
    fun `utførSteg skal avslutte behandling og oppdatere fagsak status til løpende når behandling har løpende utbetaling`() {
        val tilkjentYtelse =
            lagInitieltTilkjentYtelse(behandling).also {
                it.andelerTilkjentYtelse.add(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = it,
                        behandling = behandling,
                        stønadTom = YearMonth.now().plusMonths(2),
                    ),
                )
            }
        every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse
        val fagsakStausSlot = slot<FagsakStatus>()
        every { fagsakService.oppdaterStatus(behandling.fagsak, capture(fagsakStausSlot)) } returns mockk()

        assertDoesNotThrow { avsluttBehandlingSteg.utførSteg(behandling.id) }

        verify(exactly = 1) { loggService.opprettAvsluttBehandlingLogg(behandling) }
        verify(exactly = 1) { fagsakService.oppdaterStatus(any(), any()) }

        assertEquals(FagsakStatus.LØPENDE, fagsakStausSlot.captured)
        assertEquals(BehandlingStatus.AVSLUTTET, behandling.status)
    }

    @Test
    fun `utførSteg skal avslutte behandling og oppdatere fagsak status til avsluttet når behandling ikke har løpende utbetaling`() {
        val tilkjentYtelse =
            lagInitieltTilkjentYtelse(behandling).also {
                it.andelerTilkjentYtelse.add(
                    lagAndelTilkjentYtelse(
                        tilkjentYtelse = it,
                        behandling = behandling,
                        stønadTom = YearMonth.now().minusMonths(1),
                    ),
                )
            }
        every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse
        val fagsakStausSlot = slot<FagsakStatus>()
        every { fagsakService.oppdaterStatus(behandling.fagsak, capture(fagsakStausSlot)) } returns mockk()

        assertDoesNotThrow { avsluttBehandlingSteg.utførSteg(behandling.id) }

        verify(exactly = 1) { loggService.opprettAvsluttBehandlingLogg(behandling) }
        verify(exactly = 1) { fagsakService.oppdaterStatus(any(), any()) }

        assertEquals(FagsakStatus.AVSLUTTET, fagsakStausSlot.captured)
        assertEquals(BehandlingStatus.AVSLUTTET, behandling.status)
    }

    @Test
    fun `utførSteg skal avslutte behandling men ikke oppdatere fagsak status når behandling resultat er AVSLÅTT`() {
        behandling.resultat = Behandlingsresultat.AVSLÅTT
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        assertDoesNotThrow { avsluttBehandlingSteg.utførSteg(behandling.id) }

        verify(exactly = 1) { loggService.opprettAvsluttBehandlingLogg(behandling) }
        verify(exactly = 0) { fagsakService.oppdaterStatus(any(), any()) }

        assertEquals(BehandlingStatus.AVSLUTTET, behandling.status)
    }
}
