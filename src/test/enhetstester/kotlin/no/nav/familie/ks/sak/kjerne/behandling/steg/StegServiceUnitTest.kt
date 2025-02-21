package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BesluttVedtakDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class StegServiceUnitTest {
    private val steg = mockk<List<IBehandlingSteg>>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val tilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    private val sakStatistikkService = mockk<SakStatistikkService>()
    private val taskService = mockk<TaskService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val loggService = mockk<LoggService>()

    private val stegService =
        StegService(
            steg = steg,
            behandlingRepository = behandlingRepository,
            vedtakRepository = vedtakRepository,
            tilbakekrevingRepository = tilbakekrevingRepository,
            sakStatistikkService = sakStatistikkService,
            taskService = taskService,
            loggService = loggService,
            behandlingService = behandlingService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    private val behandling = lagBehandling(id = 1, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `settBehandlingstegTilstandPåVent - skal sette årsak frist og status på behandlingstegtilstand til nåværende behandlingssteg`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
        every { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) } just runs

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

        verify(exactly = 1) { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal oppdatere årsak og eller frist på behandlingstegtilstand til nåværende behandlingssteg og returnere gamle verdier for frist og årsak`() {
        val behandlingSlot = slot<Behandling>()

        every { behandlingRepository.saveAndFlush(capture(behandlingSlot)) } returns mockk()
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
        every { loggService.opprettOppdaterVentingLogg(any(), any(), any()) } just runs
        every { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        val årsak = VenteÅrsak.AVVENTER_DOKUMENTASJON

        stegService.settBehandlingstegPåVent(
            behandling,
            frist,
            årsak,
        )

        val nyFrist = LocalDate.now().plusMonths(1)

        val gammelFrist =
            stegService.oppdaterBehandlingstegFristOgÅrsak(
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
        verify(exactly = 2) { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
    }

    @Test
    fun `oppdaterBehandlingstegTilstandPåVent - skal ikke oppdatere årsak og eller frist på behandlingstegtilstand når frist og årsak er uendret`() {
        every { behandlingRepository.saveAndFlush(any()) } returns mockk()
        every { loggService.opprettSettPåVentLogg(any(), any()) } just runs
        every { loggService.opprettOppdaterVentingLogg(any(), any(), any()) } just runs
        every { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) } just runs

        val frist = LocalDate.now().plusWeeks(1)
        val årsak = VenteÅrsak.AVVENTER_DOKUMENTASJON

        stegService.settBehandlingstegPåVent(
            behandling,
            frist,
            årsak,
        )

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
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
        verify(exactly = 1) { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
    }

    @Test
    fun `skal iverksette mot oppdrag hvis det er endring i ordinære andeler`() {
        // Arrange
        val forrigeBehandling = lagBehandling(id = 0, fagsak = behandling.fagsak, resultat = Behandlingsresultat.INNVILGET)
        val forrigeBehandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        val behandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 2000,
                ),
            )

        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeBehandlingAndeler
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns behandlingAndeler
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(any()) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)
    }

    @Test
    fun `skal iverksette mot oppdrag hvis det er endring i overgangsordningandeler`() {
        // Arrange
        val forrigeBehandling = lagBehandling(id = 0, fagsak = behandling.fagsak, resultat = Behandlingsresultat.INNVILGET)
        val forrigeBehandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(4),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        val behandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(4),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 1500,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeBehandlingAndeler
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns behandlingAndeler
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(any()) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)
    }

    @Test
    fun `skal iverksette mot oppdrag hvis det mangler andeler i ny behandling`() {
        // Arrange
        val forrigeBehandling = lagBehandling(id = 0, fagsak = behandling.fagsak, resultat = Behandlingsresultat.INNVILGET)
        val forrigeBehandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 2000,
                ),
            )

        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeBehandlingAndeler
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(any()) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)
    }

    @Test
    fun `skal ikke iverksette mot oppdrag hvis det ikke er endring i andeler`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = behandling.fagsak, resultat = Behandlingsresultat.INNVILGET)
        val forrigeBehandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(4),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        val behandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(4),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(4),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        val taskSlot = slot<Task>()

        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeBehandlingAndeler
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns behandlingAndeler
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(capture(taskSlot)) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.JOURNALFØR_VEDTAKSBREV)
        assertThat(taskSlot.captured.type).isEqualTo("journalførVedtaksbrev")
    }

    @Test
    fun `skal iverksette mot oppdrag sålenge det er andeler på en førstegangsbehandling`() {
        // Arrange
        val behandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(3),
                    kalkulertUtbetalingsbeløp = 1000,
                ),
            )

        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns null
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns behandlingAndeler
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(any()) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)
    }

    @Test
    fun `skal ikke iverksette hvis det mangler andeler på behandlingen`() {
        // Arrange
        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns null
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns emptyList()
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(any()) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.JOURNALFØR_VEDTAKSBREV)
    }

    @Test
    fun `skal ikke iverksette mot oppdrag hvis det ikke er endring i overgangsordningandeler, men ikke i totalbeløp for andelene`() {
        // Arrange
        val forrigeBehandling = lagBehandling(fagsak = behandling.fagsak, resultat = Behandlingsresultat.INNVILGET)
        val forrigeBehandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(4),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                lagAndelTilkjentYtelse(
                    behandling = forrigeBehandling,
                    stønadFom = YearMonth.now().minusMonths(3),
                    stønadTom = YearMonth.now().minusMonths(2),
                    kalkulertUtbetalingsbeløp = 500,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        val behandlingAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(5),
                    stønadTom = YearMonth.now().minusMonths(4),
                    kalkulertUtbetalingsbeløp = 500,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                lagAndelTilkjentYtelse(
                    behandling = behandling,
                    stønadFom = YearMonth.now().minusMonths(3),
                    stønadTom = YearMonth.now().minusMonths(2),
                    kalkulertUtbetalingsbeløp = 1000,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns forrigeBehandling
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(forrigeBehandling.id) } returns forrigeBehandlingAndeler
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns behandlingAndeler
        every { vedtakRepository.findByBehandlingAndAktiv(any()) } returns lagVedtak()
        every { taskService.save(any()) } returns mockk()

        val behandlingsStegDto =
            BesluttVedtakDto(
                Beslutning.GODKJENT,
                "GODKJENT",
            )

        // Act
        val nesteSteg = stegService.hentNesteSteg(behandling, BehandlingSteg.BESLUTTE_VEDTAK, behandlingsStegDto)

        // Assert
        assertThat(nesteSteg).isEqualTo(BehandlingSteg.JOURNALFØR_VEDTAKSBREV)
    }
}
