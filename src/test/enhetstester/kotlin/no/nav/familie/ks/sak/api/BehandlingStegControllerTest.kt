package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class BehandlingStegControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val stegService = mockk<StegService>()
    private val behandlingService = mockk<BehandlingService>()
    private val behandlingStegController =
        BehandlingStegController(
            tilgangService = tilgangService,
            stegService = stegService,
            behandlingService = behandlingService,
        )

    private val behandlingId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        justRun { stegService.utførSteg(any(), any(), any()) }
        every { behandlingService.lagBehandlingRespons(any()) } returns mockk()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stegEndepunkter")
    fun `skal validere tilgang til fagsaken for behandlingen før steget utføres`(
        handling: String,
        minimumBehandlerRolle: BehandlerRolle,
        kallEndepunkt: (BehandlingStegController, Long) -> Any,
    ) {
        // Act
        kallEndepunkt(behandlingStegController, behandlingId)

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandlingId,
                event = AuditLoggerEvent.UPDATE,
                minimumBehandlerRolle = minimumBehandlerRolle,
                handling = handling,
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stegEndepunkter")
    fun `skal ikke utføre steget når saksbehandler mangler tilgang til fagsaken`(
        handling: String,
        minimumBehandlerRolle: BehandlerRolle,
        kallEndepunkt: (BehandlingStegController, Long) -> Any,
    ) {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                behandlingId = behandlingId,
                event = AuditLoggerEvent.UPDATE,
                minimumBehandlerRolle = minimumBehandlerRolle,
                handling = handling,
            )
        } throws RolleTilgangskontrollFeil("Mangler tilgang til fagsaken")

        // Act & Assert
        val feilmelding =
            assertThrows<RolleTilgangskontrollFeil> {
                kallEndepunkt(behandlingStegController, behandlingId)
            }.message

        assertThat(feilmelding).isEqualTo("Mangler tilgang til fagsaken")
        verify(exactly = 0) { stegService.utførSteg(any(), any(), any()) }
    }

    companion object {
        @JvmStatic
        fun stegEndepunkter(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "registrere søknad",
                    BehandlerRolle.SAKSBEHANDLER,
                    { controller: BehandlingStegController, id: Long -> controller.registrereSøknadOgOppdaterPersongrunnlag(id, mockk(relaxed = true)) },
                ),
                Arguments.of(
                    "utfør vilkårsvurdering",
                    BehandlerRolle.SAKSBEHANDLER,
                    { controller: BehandlingStegController, id: Long -> controller.utførVilkårsvurdering(id) },
                ),
                Arguments.of(
                    "vurdere behandlingsresultat",
                    BehandlerRolle.SAKSBEHANDLER,
                    { controller: BehandlingStegController, id: Long -> controller.utledBehandlingsresultat(id) },
                ),
                Arguments.of(
                    "fullfør simulering",
                    BehandlerRolle.SAKSBEHANDLER,
                    { controller: BehandlingStegController, id: Long -> controller.fullførSimulering(id, mockk(relaxed = true)) },
                ),
                Arguments.of(
                    "foreslå vedtak",
                    BehandlerRolle.SAKSBEHANDLER,
                    { controller: BehandlingStegController, id: Long -> controller.foreslåVedtak(id, "4812") },
                ),
                Arguments.of(
                    "beslutt vedtak",
                    BehandlerRolle.BESLUTTER,
                    { controller: BehandlingStegController, id: Long -> controller.besluttVedtak(id, mockk(relaxed = true)) },
                ),
            )
    }
}
