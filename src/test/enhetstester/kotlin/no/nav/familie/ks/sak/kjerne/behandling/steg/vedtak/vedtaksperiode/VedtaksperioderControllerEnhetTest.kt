package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.brev.BrevPeriodeService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.FritekstBegrunnelseDto
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksperioderControllerEnhetTest {
    private val tilgangService = mockk<TilgangService>()
    private val brevPeriodeService = mockk<BrevPeriodeService>()
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()

    private val vedtaksperioderController =
        VedtaksperioderController(
            tilgangService = tilgangService,
            brevPeriodeService = brevPeriodeService,
            brevKlient = mockk(),
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
        )

    private val behandlingId = 10L
    private val vedtaksperiodeId = 20L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { vedtaksperiodeHentOgPersisterService.finnBehandlingIdFor(vedtaksperiodeId) } returns behandlingId
        every { brevPeriodeService.hentBegrunnelsesteksterForPeriode(vedtaksperiodeId) } returns listOf(FritekstBegrunnelseDto(fritekst = "Fritekst"))
    }

    @Test
    fun `skal validere tilgang til behandlingen som vedtaksperioden tilhører før begrunnelsene hentes`() {
        // Act
        val begrunnelser = vedtaksperioderController.genererBegrunnelserForPeriode(vedtaksperiodeId).body?.data

        // Assert
        assertThat(begrunnelser).containsExactly("Fritekst")
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandlingId,
                event = AuditLoggerEvent.ACCESS,
                minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                handling = "hente begrunnelsetekster",
            )
        }
    }

    @Test
    fun `skal ikke hente begrunnelser når saksbehandler mangler tilgang til behandlingen`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.ACCESS, BehandlerRolle.VEILEDER, any())
        } throws RolleTilgangskontrollFeil("Mangler tilgang til behandlingen")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            vedtaksperioderController.genererBegrunnelserForPeriode(vedtaksperiodeId)
        }

        verify(exactly = 0) { brevPeriodeService.hentBegrunnelsesteksterForPeriode(any()) }
    }

    @Test
    fun `skal gi funksjonell feil når vedtaksperioden ikke finnes`() {
        // Arrange
        every { vedtaksperiodeHentOgPersisterService.finnBehandlingIdFor(vedtaksperiodeId) } returns null

        // Act & Assert
        assertThrows<FunksjonellFeil> {
            vedtaksperioderController.genererBegrunnelserForPeriode(vedtaksperiodeId)
        }

        verify(exactly = 0) { brevPeriodeService.hentBegrunnelsesteksterForPeriode(any()) }
    }
}
