package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ks.sak.api.dto.LeggTilBarnDto
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.LeggTilBarnService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.SettBehandlingPåVentService
import no.nav.familie.ks.sak.kjerne.behandling.steg.henleggbehandling.HenleggBehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingControllerEnhetTest {
    private val opprettBehandlingService = mockk<OpprettBehandlingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilgangService = mockk<TilgangService>()
    private val settBehandlingPåVentService = mockk<SettBehandlingPåVentService>()
    private val henleggBehandlingService = mockk<HenleggBehandlingService>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val leggTilBarnService = mockk<LeggTilBarnService>()
    private val behandlingController =
        BehandlingController(
            opprettBehandlingService = opprettBehandlingService,
            behandlingService = behandlingService,
            tilgangService = tilgangService,
            settBehandlingPåVentService = settBehandlingPåVentService,
            henleggBehandlingService = henleggBehandlingService,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            leggTilBarnService = leggTilBarnService,
        )

    @Nested
    inner class LeggTilBarn {
        private val behandlingId = 1L
        private val barnIdent = randomFnr()
        private val leggTilBarnDto = LeggTilBarnDto(barnIdent = barnIdent)

        @BeforeEach
        fun setUp() {
            justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
            justRun { tilgangService.validerTilgangTilHandlingOgPersoner(any(), any(), any(), any()) }
            justRun { leggTilBarnService.leggTilBarn(any(), any()) }
            every { behandlingService.lagBehandlingRespons(behandlingId) } returns mockk()
        }

        @Test
        fun `skal sjekke tilgang til barnet før det legges til på behandlingen`() {
            // Act
            behandlingController.leggTilBarn(behandlingId, leggTilBarnDto)

            // Assert
            verifyOrder {
                tilgangService.validerTilgangTilHandlingOgPersoner(
                    personIdenter = listOf(barnIdent),
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "legge til barn",
                )
                leggTilBarnService.leggTilBarn(behandlingId, barnIdent)
            }
        }

        @Test
        fun `skal ikke legge til barnet når saksbehandler mangler tilgang til barnet`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandlingOgPersoner(
                    personIdenter = listOf(barnIdent),
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "legge til barn",
                )
            } throws RolleTilgangskontrollFeil("Mangler tilgang til barnet")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    behandlingController.leggTilBarn(behandlingId, leggTilBarnDto)
                }.message

            assertThat(feilmelding).isEqualTo("Mangler tilgang til barnet")
            verify(exactly = 0) { leggTilBarnService.leggTilBarn(any(), any()) }
        }
    }
}
