package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.GenererVedtaksperioderForOverstyrtEndringstidspunktDto
import no.nav.familie.ks.sak.api.dto.VedtaksperiodeMedFriteksterDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserControllerEnhetTest {
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilgangService = mockk<TilgangService>()
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()

    private val vedtaksperiodeMedBegrunnelserController =
        VedtaksperiodeMedBegrunnelserController(
            vedtaksperiodeService = vedtaksperiodeService,
            behandlingService = behandlingService,
            tilgangService = tilgangService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
        )

    private val behandling = lagBehandling(id = 10L)
    private val vedtak = lagVedtak(behandling = behandling)
    private val vedtaksperiodeId = 20L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(vedtaksperiodeId) } returns lagVedtaksperiodeMedBegrunnelser(vedtak = vedtak)
        every { vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(vedtaksperiodeId, any()) } returns vedtak
        every { vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelserDto(behandlingId = behandling.id) } returns emptyList()
        justRun { vedtaksperiodeService.genererVedtaksperiodeForOverstyrtEndringstidspunkt(any(), any()) }
        every { behandlingService.lagBehandlingRespons(behandlingId = behandling.id) } returns mockk()
    }

    @Test
    fun `skal validere tilgang til behandlingen som vedtaksperioden tilhører ved oppdatering av fritekster`() {
        // Act
        vedtaksperiodeMedBegrunnelserController.oppdaterVedtaksperiodeMedFritekster(vedtaksperiodeId, VedtaksperiodeMedFriteksterDto(fritekster = listOf("Fritekst")))

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandling.id,
                event = AuditLoggerEvent.UPDATE,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "oppdatere vedtaksperiode med fritekster",
            )
        }
    }

    @Test
    fun `skal ikke oppdatere fritekster når saksbehandler mangler tilgang til behandlingen`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandling.id, AuditLoggerEvent.UPDATE, BehandlerRolle.SAKSBEHANDLER, any())
        } throws RolleTilgangskontrollFeil("Mangler tilgang til behandlingen")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            vedtaksperiodeMedBegrunnelserController.oppdaterVedtaksperiodeMedFritekster(vedtaksperiodeId, VedtaksperiodeMedFriteksterDto(fritekster = listOf("Fritekst")))
        }

        verify(exactly = 0) { vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(any(), any()) }
    }

    @Test
    fun `skal kreve saksbehandlerrolle og tilgang til behandlingen for å generere vedtaksperioder`() {
        // Act
        vedtaksperiodeMedBegrunnelserController.genererVedtaksperioderTilOgMedFørsteEndringstidspunkt(
            GenererVedtaksperioderForOverstyrtEndringstidspunktDto(behandlingId = behandling.id, overstyrtEndringstidspunkt = LocalDate.of(2025, 1, 1)),
        )

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandling.id,
                event = AuditLoggerEvent.UPDATE,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = "Generer vedtaksperioder",
            )
        }
    }
}
