package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagBeregnetUtbetalingsoppdrag
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagTilkjentYtelse
import no.nav.familie.ks.sak.data.lagUtbetalingsoppdrag
import no.nav.familie.ks.sak.data.lagUtbetalingsperiode
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class UtbetalingsperiodeServiceTest {
    @MockK
    private lateinit var oppdragKlient: OppdragKlient

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @MockK
    private lateinit var utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @InjectMockKs
    private lateinit var utbetalingsoppdragService: UtbetalingsoppdragService

    private val fagsak = lagFagsak()

    private val behandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    private val vedtak = Vedtak(behandling = behandling)

    @BeforeEach
    fun beforeEach() {
        every { behandlingService.hentSisteBehandlingSomErIverksatt(any()) } returns null
        every { tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(any()) } just runs
        every { oppdragKlient.iverksettOppdrag(any()) } returns ""
        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(any()) } returns lagTilkjentYtelse(utbetalingsoppdrag = lagUtbetalingsoppdrag(emptyList()), behandling = behandling).also { it.andelerTilkjentYtelse.addAll(mutableListOf(lagAndelTilkjentYtelse())) }
        every { tilkjentYtelseRepository.save(any()) } returns mockk()
        every { tilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(any()) } returns mockk()
        every { andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(any()) } returns emptyList()
    }

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal ikke iverksette mot oppdrag hvis det ikke finnes utbetalingsperioder`() {
        every {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            lagBeregnetUtbetalingsoppdrag(vedtak, utbetlingsperioder = emptyList())
        utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak,
            "",
        )
        verify(exactly = 0) { oppdragKlient.iverksettOppdrag(any()) }
    }

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal iverksette mot oppdrag hvis det finnes utbetalingsperioder`() {
        every {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns lagBeregnetUtbetalingsoppdrag(vedtak, utbetlingsperioder = listOf(lagUtbetalingsperiode(vedtak)))
        utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak,
            "",
        )
        verify(exactly = 1) { oppdragKlient.iverksettOppdrag(any()) }
    }
}
