package no.nav.familie.ks.sak.kjerne.avstemming

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
internal class AvstemmingServiceTest {
    @MockK
    private lateinit var oppdragKlient: OppdragKlient

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var beregningService: BeregningService

    @InjectMockKs
    private lateinit var avstemmingService: AvstemmingService

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
