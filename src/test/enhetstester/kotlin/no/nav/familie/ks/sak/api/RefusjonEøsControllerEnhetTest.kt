package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.RefusjonEøsDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RefusjonEøsControllerEnhetTest {
    private val tilgangService = mockk<TilgangService>()
    private val refusjonEøsService = mockk<RefusjonEøsService>()
    private val behandlingService = mockk<BehandlingService>()

    private val refusjonEøsController =
        RefusjonEøsController(
            tilgangService = tilgangService,
            refusjonEøsService = refusjonEøsService,
            behandlingService = behandlingService,
        )

    private val behandlingId = 1L
    private val periodeId = 2L
    private val refusjonEøsDto =
        RefusjonEøsDto(
            id = periodeId,
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 12, 31),
            refusjonsbeløp = 100,
            land = "SE",
            refusjonAvklart = true,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { refusjonEøsService.leggTilRefusjonEøsPeriode(any(), any()) } returns periodeId
        every { refusjonEøsService.oppdaterRefusjonEøsPeriode(any(), any(), any()) } returns mockk()
        justRun { refusjonEøsService.fjernRefusjonEøsPeriode(any(), any()) }
        every { refusjonEøsService.hentRefusjonEøsPerioder(any()) } returns emptyList()
        every { behandlingService.lagBehandlingRespons(behandlingId = behandlingId) } returns mockk()
    }

    @Test
    fun `skal validere tilgang til fagsaken for behandlingen på alle endepunkter`() {
        // Act
        refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId = behandlingId, refusjonEøs = refusjonEøsDto)
        refusjonEøsController.oppdaterRefusjonEøsPeriode(behandlingId = behandlingId, id = periodeId, refusjonEøs = refusjonEøsDto)
        refusjonEøsController.fjernRefusjonEøsPeriode(behandlingId = behandlingId, id = periodeId)
        refusjonEøsController.hentRefusjonEøsPerioder(behandlingId = behandlingId)

        // Assert
        verify(exactly = 1) { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.CREATE, BehandlerRolle.SAKSBEHANDLER, any()) }
        verify(exactly = 1) { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.UPDATE, BehandlerRolle.SAKSBEHANDLER, any()) }
        verify(exactly = 1) { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.DELETE, BehandlerRolle.SAKSBEHANDLER, any()) }
        verify(exactly = 1) { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.ACCESS, BehandlerRolle.VEILEDER, any()) }
        verify(exactly = 1) { refusjonEøsService.oppdaterRefusjonEøsPeriode(refusjonEøs = refusjonEøsDto, id = periodeId, behandlingId = behandlingId) }
    }

    @Test
    fun `skal ikke legge til periode eller returnere behandlingen når saksbehandler mangler tilgang`() {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(behandlingId, AuditLoggerEvent.CREATE, BehandlerRolle.SAKSBEHANDLER, any())
        } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsaken")

        // Act & Assert
        assertThrows<RolleTilgangskontrollFeil> {
            refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId = behandlingId, refusjonEøs = refusjonEøsDto)
        }

        verify(exactly = 0) { refusjonEøsService.leggTilRefusjonEøsPeriode(any(), any()) }
        verify(exactly = 0) { behandlingService.lagBehandlingRespons(any()) }
    }
}
