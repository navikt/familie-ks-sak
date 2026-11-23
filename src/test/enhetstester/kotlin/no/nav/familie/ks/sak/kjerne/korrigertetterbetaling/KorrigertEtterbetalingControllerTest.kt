package no.nav.familie.ks.sak.kjerne.korrigertetterbetaling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.common.exception.RolleTilgangskontrollFeil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KorrigertEtterbetalingControllerTest {
    private val tilgangService = mockk<TilgangService>()
    private val korrigertEtterbetalingService = mockk<KorrigertEtterbetalingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val korrigertEtterbetalingController =
        KorrigertEtterbetalingController(
            tilgangService = tilgangService,
            korrigertEtterbetalingService = korrigertEtterbetalingService,
            behandlingService = behandlingService,
        )

    private val behandlingId = 1L
    private val korrigertEtterbetaling =
        lagKorrigertEtterbetaling(
            behandling = lagBehandling(id = behandlingId),
            begrunnelse = "Motregnet mot feilutbetaling",
            beløp = 2500,
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
        every { behandlingService.hentBehandling(behandlingId) } returns mockk()
        every { korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(any()) } returns null
    }

    @Nested
    inner class HentKorrigertEtterbetalingPåBehandling {
        @Test
        fun `skal returnere aktiv korrigert etterbetaling på behandlingen`() {
            // Arrange
            every { korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandlingId) } returns korrigertEtterbetaling

            // Act
            val respons = korrigertEtterbetalingController.hentKorrigertEtterbetalingPåBehandling(behandlingId)

            // Assert
            assertThat(respons.body?.data?.beløp).isEqualTo(korrigertEtterbetaling.beløp)
            assertThat(respons.body?.data?.begrunnelse).isEqualTo(korrigertEtterbetaling.begrunnelse)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har aktiv korrigert etterbetaling`() {
            // Arrange
            every { korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(behandlingId) } returns null

            // Act
            val respons = korrigertEtterbetalingController.hentKorrigertEtterbetalingPåBehandling(behandlingId)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }
    }

    @Nested
    inner class SettKorrigertEtterbetalingTilInaktivPåBehandling {
        @Test
        fun `skal kreve saksbehandlerrolle og tilgang til fagsaken`() {
            // Act
            korrigertEtterbetalingController.settKorrigertEtterbetalingTilInaktivPåBehandling(behandlingId)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandlingId = behandlingId,
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "Oppdater korrigert etterbetaling",
                )
            }
            verify(exactly = 1) { korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(any()) }
        }

        @Test
        fun `skal ikke sette korrigering til inaktiv når kaller ikke er saksbehandler`() {
            // Arrange
            every {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandlingId = behandlingId,
                    event = AuditLoggerEvent.UPDATE,
                    minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                    handling = "Oppdater korrigert etterbetaling",
                )
            } throws RolleTilgangskontrollFeil("Krever SAKSBEHANDLER")

            // Act & Assert
            val feilmelding =
                assertThrows<RolleTilgangskontrollFeil> {
                    korrigertEtterbetalingController.settKorrigertEtterbetalingTilInaktivPåBehandling(behandlingId)
                }.message

            assertThat(feilmelding).isEqualTo("Krever SAKSBEHANDLER")
            verify(exactly = 0) { korrigertEtterbetalingService.settKorrigeringPåBehandlingTilInaktiv(any()) }
        }
    }
}
