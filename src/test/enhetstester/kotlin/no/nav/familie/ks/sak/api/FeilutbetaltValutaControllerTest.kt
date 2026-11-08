package no.nav.familie.ks.sak.api

import io.mockk.Called
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.FeilutbetaltValutaDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

class FeilutbetaltValutaControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val feilutbetaltValutaService = mockk<FeilutbetaltValutaService>(relaxed = true)
    private val behandlingService = mockk<BehandlingService>()
    private val feilutbetaltValutaController =
        FeilutbetaltValutaController(
            tilgangService = tilgangService,
            feilutbetaltValutaService = feilutbetaltValutaService,
            behandlingService = behandlingService,
        )

    private val behandlingId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { behandlingService.lagBehandlingRespons(any()) } returns mockk()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    fun `skal validere rolle og tilgang til fagsaken for handlingen`(
        handling: String,
        minimumBehandlerRolle: BehandlerRolle,
        event: AuditLoggerEvent,
        kallEndepunkt: (FeilutbetaltValutaController, Long) -> Any,
    ) {
        // Act
        kallEndepunkt(feilutbetaltValutaController, behandlingId)

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
    fun `skal ikke røre feilutbetalt valuta når kaller mangler tilgang`(
        handling: String,
        minimumBehandlerRolle: BehandlerRolle,
        event: AuditLoggerEvent,
        kallEndepunkt: (FeilutbetaltValutaController, Long) -> Any,
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
                kallEndepunkt(feilutbetaltValutaController, behandlingId)
            }.message

        assertThat(feilmelding).isEqualTo("Mangler tilgang")
        verify { feilutbetaltValutaService wasNot Called }
    }

    companion object {
        private val feilutbetaltValutaDto =
            FeilutbetaltValutaDto(
                id = null,
                fom = LocalDate.of(2026, 1, 1),
                tom = LocalDate.of(2026, 1, 31),
                feilutbetaltBeløp = 100,
            )

        @JvmStatic
        fun endepunkter(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "legg til feilutbetalt valuta med periode til behandling",
                    BehandlerRolle.SAKSBEHANDLER,
                    AuditLoggerEvent.CREATE,
                    { controller: FeilutbetaltValutaController, id: Long -> controller.leggTilFeilutbetaltValuta(id, feilutbetaltValutaDto) },
                ),
                Arguments.of(
                    "oppdater feilutbetalt valuta i behandling",
                    BehandlerRolle.SAKSBEHANDLER,
                    AuditLoggerEvent.UPDATE,
                    { controller: FeilutbetaltValutaController, id: Long -> controller.oppdaterFeilutbetaltValuta(id, 2L, feilutbetaltValutaDto) },
                ),
                Arguments.of(
                    "Fjerner feilutbetalt valuta i behandling",
                    BehandlerRolle.SAKSBEHANDLER,
                    AuditLoggerEvent.DELETE,
                    { controller: FeilutbetaltValutaController, id: Long -> controller.fjernFeilutbetaltValuta(id, 2L) },
                ),
                Arguments.of(
                    "henter alle feilutbetalt valuta for behandling",
                    BehandlerRolle.VEILEDER,
                    AuditLoggerEvent.ACCESS,
                    { controller: FeilutbetaltValutaController, id: Long -> controller.hentAlleFeilutbetaltValutaForBehandling(id) },
                ),
            )
    }
}
