package no.nav.familie.ks.sak.api

import io.mockk.Called
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.RefusjonEøsDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

class RefusjonEøsControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val refusjonEøsService = mockk<RefusjonEøsService>(relaxed = true)
    private val refusjonEøsController =
        RefusjonEøsController(
            tilgangService = tilgangService,
            refusjonEøsService = refusjonEøsService,
        )

    private val behandlingId = 1L
    private val lagredePerioder =
        listOf(
            refusjonEøsDto.copy(id = 2L, refusjonsbeløp = 700),
            refusjonEøsDto.copy(id = 3L, refusjonsbeløp = 900, land = "DK"),
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `skal returnere oppdaterte perioder etter oppretting`() {
        // Arrange
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns lagredePerioder

        // Act
        val respons = refusjonEøsController.leggTilRefusjonEøsPeriode(behandlingId, refusjonEøsDto)

        // Assert
        assertThat(respons.body?.data).isEqualTo(lagredePerioder)
    }

    @Test
    fun `skal returnere oppdaterte perioder etter oppdatering`() {
        // Arrange
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns lagredePerioder

        // Act
        val respons = refusjonEøsController.oppdaterRefusjonEøsPeriode(behandlingId, 2L, refusjonEøsDto)

        // Assert
        assertThat(respons.body?.data).isEqualTo(lagredePerioder)
    }

    @Test
    fun `skal returnere gjenværende perioder etter sletting`() {
        // Arrange
        val gjenværendePerioder = lagredePerioder.drop(1)
        every { refusjonEøsService.hentRefusjonEøsPerioder(behandlingId) } returns gjenværendePerioder

        // Act
        val respons = refusjonEøsController.fjernRefusjonEøsPeriode(behandlingId, 2L)

        // Assert
        assertThat(respons.body?.data).isEqualTo(gjenværendePerioder)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    fun `skal validere rolle og tilgang til fagsaken for handlingen`(
        handling: String,
        minimumBehandlerRolle: BehandlerRolle,
        event: AuditLoggerEvent,
        kallEndepunkt: (RefusjonEøsController, Long) -> Any,
    ) {
        // Act
        kallEndepunkt(refusjonEøsController, behandlingId)

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandlingId,
                event = event,
                minimumBehandlerRolle = minimumBehandlerRolle,
                handling = handling,
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    fun `skal ikke røre refusjon EØS når kaller mangler tilgang`(
        handling: String,
        minimumBehandlerRolle: BehandlerRolle,
        event: AuditLoggerEvent,
        kallEndepunkt: (RefusjonEøsController, Long) -> Any,
    ) {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandlingId,
                event = event,
                minimumBehandlerRolle = minimumBehandlerRolle,
                handling = handling,
            )
        } throws RolleTilgangskontrollFeil("Mangler tilgang")

        // Act & Assert
        val feilmelding =
            assertThrows<RolleTilgangskontrollFeil> {
                kallEndepunkt(refusjonEøsController, behandlingId)
            }.message

        assertThat(feilmelding).isEqualTo("Mangler tilgang")
        verify { refusjonEøsService wasNot Called }
    }

    companion object {
        private val refusjonEøsDto =
            RefusjonEøsDto(
                id = null,
                fom = LocalDate.of(2026, 1, 1),
                tom = LocalDate.of(2026, 1, 31),
                refusjonsbeløp = 100,
                land = "SE",
                refusjonAvklart = true,
            )

        @JvmStatic
        fun endepunkter(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "legg til periode med refusjon EØS",
                    BehandlerRolle.SAKSBEHANDLER,
                    AuditLoggerEvent.CREATE,
                    { controller: RefusjonEøsController, id: Long -> controller.leggTilRefusjonEøsPeriode(id, refusjonEøsDto) },
                ),
                Arguments.of(
                    "oppdater periode med refusjon EØS",
                    BehandlerRolle.SAKSBEHANDLER,
                    AuditLoggerEvent.UPDATE,
                    { controller: RefusjonEøsController, id: Long -> controller.oppdaterRefusjonEøsPeriode(id, 2L, refusjonEøsDto) },
                ),
                Arguments.of(
                    "fjerner periode med refusjon EØS",
                    BehandlerRolle.SAKSBEHANDLER,
                    AuditLoggerEvent.DELETE,
                    { controller: RefusjonEøsController, id: Long -> controller.fjernRefusjonEøsPeriode(id, 2L) },
                ),
                Arguments.of(
                    "hente refusjon EØS for behandling",
                    BehandlerRolle.VEILEDER,
                    AuditLoggerEvent.ACCESS,
                    { controller: RefusjonEøsController, id: Long -> controller.hentRefusjonEøsPerioder(id) },
                ),
            )
    }
}
