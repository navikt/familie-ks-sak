package no.nav.familie.ks.sak.api

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKorrigertVedtak
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ks.sak.sikkerhet.AuditLoggerEvent
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KorrigertVedtakControllerTest {
    private val behandlingService = mockk<BehandlingService>()
    private val korrigertVedtakService = mockk<KorrigertVedtakService>()
    private val tilgangService = mockk<TilgangService>()
    private val korrigertVedtakController =
        KorrigertVedtakController(
            behandlingService = behandlingService,
            korrigertVedtakService = korrigertVedtakService,
            tilgangService = tilgangService,
        )

    private val behandling = lagBehandling()
    private val korrigertVedtak =
        lagKorrigertVedtak(
            behandling = behandling,
            vedtaksdato = LocalDate.of(2026, 1, 1),
            begrunnelse = "Feil i tidligere vedtak",
        )

    @BeforeEach
    fun setUp() {
        justRun { tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(any(), any(), any(), any()) }
    }

    @Nested
    inner class HentKorrigertVedtakPåBehandling {
        @Test
        fun `skal returnere aktivt korrigert vedtak på behandlingen`() {
            // Arrange
            every { korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId = behandling.id) } returns korrigertVedtak

            // Act
            val respons = korrigertVedtakController.hentKorrigertVedtakPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.data?.vedtaksdato).isEqualTo(korrigertVedtak.vedtaksdato)
            assertThat(respons.body?.data?.begrunnelse).isEqualTo(korrigertVedtak.begrunnelse)
        }

        @Test
        fun `skal returnere null når behandlingen ikke har aktivt korrigert vedtak`() {
            // Arrange
            every { korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId = behandling.id) } returns null

            // Act
            val respons = korrigertVedtakController.hentKorrigertVedtakPåBehandling(behandling.id)

            // Assert
            assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
            assertThat(respons.body?.data).isNull()
        }

        @Test
        fun `skal kreve veilederrolle og tilgang til fagsaken`() {
            // Arrange
            every { korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId = behandling.id) } returns null

            // Act
            korrigertVedtakController.hentKorrigertVedtakPåBehandling(behandling.id)

            // Assert
            verify(exactly = 1) {
                tilgangService.validerTilgangTilHandlingOgFagsakForBehandling(
                    behandlingId = behandling.id,
                    event = AuditLoggerEvent.ACCESS,
                    minimumBehandlerRolle = BehandlerRolle.VEILEDER,
                    handling = "Hent korrigering på vedtak",
                )
            }
        }
    }
}
