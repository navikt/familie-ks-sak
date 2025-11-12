package no.nav.familie.ks.sak.kjerne.avstemming

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppdrag.AvstemmingKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime

internal class AvstemmingServiceTest {
    private val avstemmingKlient = mockk<AvstemmingKlient>()
    private val behandlingService = mockk<BehandlingService>()
    private val beregningService = mockk<BeregningService>()

    private val avstemmingService =
        AvstemmingService(
            avstemmingKlient = avstemmingKlient,
            behandlingService = behandlingService,
            beregningService = beregningService,
        )

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `hentDataForKonsistensavstemming skal hente data for konsistensavstemming`() {
        every { beregningService.hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(any(), any()) } returns
            listOf(lagAndelTilkjentYtelse(behandling = behandling, periodeOffset = 1))
        every { behandlingService.hentAktivtFødselsnummerForBehandlinger(any()) } returns
            mapOf(behandling.id to behandling.fagsak.aktør.aktivFødselsnummer())

        val perioderForBehandling =
            assertDoesNotThrow {
                avstemmingService.hentDataForKonsistensavstemming(LocalDateTime.now(), listOf(behandling.id))
            }
        assertTrue { perioderForBehandling.size == 1 }

        val periodeForBehandling = perioderForBehandling[0]
        assertEquals(behandling.id.toString(), periodeForBehandling.behandlingId)
        assertEquals(behandling.fagsak.aktør.aktivFødselsnummer(), periodeForBehandling.aktivFødselsnummer)
        assertTrue { periodeForBehandling.perioder.contains(1) } // Siden 1 er periodeOffset
    }
}
