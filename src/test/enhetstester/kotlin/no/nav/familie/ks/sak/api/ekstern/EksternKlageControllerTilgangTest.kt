package no.nav.familie.ks.sak.api.ekstern

import io.mockk.Called
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class EksternKlageControllerTilgangTest {
    private val tilgangService = mockk<TilgangService>()
    private val opprettBehandlingService = mockk<OpprettBehandlingService>(relaxed = true)
    private val klageService = mockk<KlageService>()
    private val eksternKlageController =
        EksternKlageController(
            tilgangService = tilgangService,
            opprettBehandlingService = opprettBehandlingService,
            klageService = klageService,
        )

    private val fagsakId = 1L

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsak(any(), any(), any(), any()) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    fun `skal kreve saksbehandlerrolle og tilgang til fagsaken`(
        handling: String,
        kallEndepunkt: (EksternKlageController, Long) -> Any,
    ) {
        // Act
        kallEndepunkt(eksternKlageController, fagsakId)

        // Assert
        verify(exactly = 1) {
            tilgangService.validerTilgangTilHandlingOgFagsak(
                fagsakId = fagsakId,
                event = AuditLoggerEvent.CREATE,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = handling,
            )
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    fun `skal ikke gå videre når kaller mangler tilgang`(
        handling: String,
        kallEndepunkt: (EksternKlageController, Long) -> Any,
    ) {
        // Arrange
        every {
            tilgangService.validerTilgangTilHandlingOgFagsak(
                fagsakId = fagsakId,
                event = AuditLoggerEvent.CREATE,
                minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                handling = handling,
            )
        } throws RolleTilgangskontrollFeil("Mangler tilgang")

        // Act & Assert
        val feilmelding =
            assertThrows<RolleTilgangskontrollFeil> {
                kallEndepunkt(eksternKlageController, fagsakId)
            }.message

        assertThat(feilmelding).isEqualTo("Mangler tilgang")
        verify { opprettBehandlingService wasNot Called }
    }

    companion object {
        @JvmStatic
        fun endepunkter(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "Valider vi kan opprette revurdering med årsak klage på fagsak=1",
                    { controller: EksternKlageController, id: Long -> controller.kanOppretteRevurderingKlage(id) },
                ),
                Arguments.of(
                    "Opprett revurdering med årsak klage på fagsak=1",
                    { controller: EksternKlageController, id: Long -> controller.opprettRevurderingKlage(id, UUID.randomUUID()) },
                ),
            )
    }
}
