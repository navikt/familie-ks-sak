package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagTilkjentYtelse
import no.nav.familie.ks.sak.data.lagUtbetalingsoppdrag
import no.nav.familie.ks.sak.data.lagUtbetalingsperiode
import no.nav.familie.ks.sak.data.lagØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.data.lagØkonomiSimuleringPostering
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottakerRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class SimuleringServiceTest {

    @MockK
    private lateinit var oppdragKlient: OppdragKlient

    @MockK
    private lateinit var utbetalingsoppdragService: UtbetalingsoppdragService

    @MockK
    private lateinit var beregningService: BeregningService

    @MockK
    private lateinit var øknomiSimuleringMottakerRepository: ØkonomiSimuleringMottakerRepository

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @InjectMockKs
    private lateinit var simuleringService: SimuleringService

    @ParameterizedTest
    @EnumSource(value = BehandlingStatus::class, names = ["IVERKSETTER_VEDTAK", "AVSLUTTET"])
    fun `oppdaterSimuleringPåBehandlingVedBehov - skal returnere eksisterende simulering dersom behandlingstatus er IVERKSETTER_VEDTAK eller AVSLUTTET`(
        behandlingStatus: BehandlingStatus
    ) {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).also { it.status = behandlingStatus }
        val eksisterendeSimulering = listOf(
            lagØkonomiSimuleringMottaker(
                behandling = behandling,
                økonomiSimuleringPosteringer = listOf(
                    lagØkonomiSimuleringPostering(
                        behandling = behandling,
                        fom = LocalDate.now().minusMonths(2),
                        tom = LocalDate.now(),
                        beløp = BigDecimal.valueOf(7500),
                        forfallsdato = LocalDate.now().plusMonths(5)
                    )
                )
            )
        )

        every { behandlingRepository.hentBehandling(behandling.id) } returns behandling
        every { øknomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns eksisterendeSimulering

        val simulering = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId = behandling.id)

        verify(exactly = 0) { øknomiSimuleringMottakerRepository.saveAll(any<List<ØkonomiSimuleringMottaker>>()) }
        assertEquals(eksisterendeSimulering, simulering)
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStatus::class,
        names = ["IVERKSETTER_VEDTAK", "AVSLUTTET"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `oppdaterSimuleringPåBehandlingVedBehov - skal returnere eksisterende simulering dersom behandlingstatus er ulik IVERKSETTER_VEDTAK og AVSLUTTET og simulering ikke er utdatert`(
        behandlingStatus: BehandlingStatus
    ) {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).also { it.status = behandlingStatus }
        val eksisterendeSimulering = listOf(
            lagØkonomiSimuleringMottaker(
                behandling = behandling,
                økonomiSimuleringPosteringer = listOf(
                    lagØkonomiSimuleringPostering(
                        behandling = behandling,
                        fom = LocalDate.now().minusMonths(2),
                        tom = LocalDate.now(),
                        beløp = BigDecimal.valueOf(7500),
                        forfallsdato = LocalDate.now().plusMonths(5)
                    ),
                    lagØkonomiSimuleringPostering(
                        behandling = behandling,
                        fom = LocalDate.now().plusMonths(2),
                        tom = LocalDate.now().plusMonths(4),
                        beløp = BigDecimal.valueOf(7500),
                        forfallsdato = LocalDate.now().plusMonths(5)
                    )
                )
            )
        )

        every { behandlingRepository.hentBehandling(behandling.id) } returns behandling
        every { øknomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns eksisterendeSimulering

        val simulering = simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId = behandling.id)

        verify(exactly = 0) { øknomiSimuleringMottakerRepository.saveAll(any<List<ØkonomiSimuleringMottaker>>()) }
        assertEquals(eksisterendeSimulering, simulering)
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStatus::class,
        names = ["IVERKSETTER_VEDTAK", "AVSLUTTET"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `oppdaterSimuleringPåBehandlingVedBehov - skal returnere hente ny simulering dersom behandlingstatus er ulik IVERKSETTER_VEDTAK og AVSLUTTET og simulering ikke er hentet fra før`(
        behandlingStatus: BehandlingStatus
    ) {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD).also { it.status = behandlingStatus }
        val nySimulering = listOf(
            SimuleringMottaker(
                mottakerType = MottakerType.BRUKER,
                mottakerNummer = "",
                simulertPostering = listOf(
                    SimulertPostering(
                        fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
                        fom = LocalDate.now().minusMonths(4),
                        tom = LocalDate.now().minusMonths(2),
                        betalingType = BetalingType.DEBIT,
                        beløp = BigDecimal.valueOf(7500),
                        posteringType = PosteringType.YTELSE,
                        forfallsdato = LocalDate.now(),
                        utenInntrekk = true
                    ),
                    SimulertPostering(
                        fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
                        fom = LocalDate.now().minusMonths(2),
                        tom = LocalDate.now(),
                        betalingType = BetalingType.DEBIT,
                        beløp = BigDecimal.valueOf(7500),
                        posteringType = PosteringType.YTELSE,
                        forfallsdato = LocalDate.now().plusMonths(2),
                        utenInntrekk = true
                    )
                )
            )
        )

        every { behandlingRepository.hentBehandling(behandling.id) } returns behandling

        // Finnes ingen lagret simulering fra før
        every { øknomiSimuleringMottakerRepository.findByBehandlingId(behandling.id) } returns emptyList()

        every { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) } returns Vedtak(behandling = behandling)
        every { beregningService.innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(behandling = behandling) } returns false
        every {
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                any(),
                any(),
                any(),
                any()
            )
        } returns lagTilkjentYtelse(
            utbetalingsoppdrag = lagUtbetalingsoppdrag(listOf(lagUtbetalingsperiode())),
            behandling = behandling
        )
        every { oppdragKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottaker = nySimulering)
        every { øknomiSimuleringMottakerRepository.deleteByBehandlingId(any()) } just runs
        every { øknomiSimuleringMottakerRepository.saveAll(any<List<ØkonomiSimuleringMottaker>>()) } returns mockk()

        simuleringService.oppdaterSimuleringPåBehandlingVedBehov(behandlingId = behandling.id)

        verify(exactly = 1) { oppdragKlient.hentSimulering(any()) }
        verify(exactly = 1) { øknomiSimuleringMottakerRepository.saveAll(any<List<ØkonomiSimuleringMottaker>>()) }
    }
}
