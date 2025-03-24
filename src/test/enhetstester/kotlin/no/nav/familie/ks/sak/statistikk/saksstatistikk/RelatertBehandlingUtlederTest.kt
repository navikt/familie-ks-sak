package no.nav.familie.ks.sak.statistikk.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKlagebehandlingDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class RelatertBehandlingUtlederTest {
    private val behandlingService = mockk<BehandlingService>()
    private val klageService = mockk<KlageService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val relatertBehandlingUtleder =
        RelatertBehandlingUtleder(
            behandlingService = behandlingService,
            klageService = klageService,
            unleashService = unleashService,
        )

    @BeforeEach
    fun oppsett() {
        every { unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false) } returns true
    }

    @Nested
    inner class UtledRelatertBehandling {
        @Test
        fun `skal returnere null når toggle for å behandle klage er skrudd av`() {
            // Arrange
            val fagsakId = 1L

            every { unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false) } returns false

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @Test
        fun `skal utlede relatert behandling når klagebehandlingen er siste vedtatte behandling`() {
            // Arrange
            val fagsakId = 1L
            val nåtidspunkt = LocalDateTime.now()

            val kontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val klagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(fagsakId) } returns klagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(klagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(klagebehandling.vedtaksdato)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["FØRSTEGANGSBEHANDLING"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede relatert behandling når kontantstøttebehandlingen er siste vedtatte behandling`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val fagsakId = 1L
            val nåtidspunkt = LocalDateTime.now()

            val kontantstøttebehandling =
                lagBehandling(
                    type = behandlingType,
                    aktivertTidspunkt = nåtidspunkt,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val klagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(1),
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(fagsakId) } returns klagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(kontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(kontantstøttebehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal ikke utlede relatert behandling når ingen behandlinger er vedtatt`() {
            // Arrange
            val fagsakId = 1L

            every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId) } returns null
            every { klageService.hentSisteVedtatteKlagebehandling(fagsakId) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @Test
        fun `skal ikke utlede relatert behandling når kun førstegangsbehandling for kontantstøtte er vedtatt`() {
            // Arrange
            val fagsakId = 1L

            val kontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTidspunkt = LocalDateTime.now(),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsakId) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(fagsakId) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling).isNull()
        }
    }
}
