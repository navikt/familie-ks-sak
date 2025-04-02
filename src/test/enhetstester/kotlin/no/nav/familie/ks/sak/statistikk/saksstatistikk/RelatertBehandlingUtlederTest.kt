package no.nav.familie.ks.sak.statistikk.saksstatistikk

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class RelatertBehandlingUtlederTest {
    private val behandlingService = mockk<BehandlingService>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val relatertBehandlingUtleder =
        RelatertBehandlingUtleder(
            behandlingService = behandlingService,
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
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false) } returns false

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { behandlingService wasNot called }
            assertThat(relatertBehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke utlede relatert behandling når den innsendte behandling er en revurdering med årsak klage eller årsak iverksette ka vedtak`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = behandlingÅrsak,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            verify { behandlingService wasNot called }
            assertThat(relatertBehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["KLAGE", "IVERKSETTE_KA_VEDTAK"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal utlede relatert behandling med kontantstøttebehandlingen som forrige vedtatte behandling når den innsendte behandling er en revurdering som ikke har årsak klage eller iverksetter ka vedtak`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = behandlingÅrsak,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val forrigeVedtatteKontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentForrigeBehandlingSomErVedtatt(revurdering) } returns forrigeVedtatteKontantstøttebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(forrigeVedtatteKontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(forrigeVedtatteKontantstøttebehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal utlede relatert behandling med kontantstøttebehandlingen som forrige vedtatte behandling når den innsendte behandling er en teknisk endring`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val tekniskEndring =
                lagBehandling(
                    type = BehandlingType.TEKNISK_ENDRING,
                    opprettetÅrsak = BehandlingÅrsak.TEKNISK_ENDRING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val forrigeVedtatteKontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentForrigeBehandlingSomErVedtatt(tekniskEndring) } returns forrigeVedtatteKontantstøttebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(tekniskEndring)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(forrigeVedtatteKontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(forrigeVedtatteKontantstøttebehandling.aktivertTidspunkt)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["REVURDERING", "TEKNISK_ENDRING"],
            mode = EnumSource.Mode.INCLUDE,
        )
        fun `skal ikke utlede relatert behandling når ingen kontantstøttebehandling er vedtatte for den innsendte revurderingen eller teknisk endringen`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    type = behandlingType,
                    opprettetÅrsak = if (behandlingType == BehandlingType.TEKNISK_ENDRING) BehandlingÅrsak.TEKNISK_ENDRING else BehandlingÅrsak.SØKNAD,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { behandlingService.hentForrigeBehandlingSomErVedtatt(behandling) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["REVURDERING", "TEKNISK_ENDRING"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        fun `skal ikke utlede relatert behandling når den innsendte behandling ikke er en revurdering eller teknisk endring`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val revurdering =
                lagBehandling(
                    type = behandlingType,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    aktivertTidspunkt = LocalDateTime.now(),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { behandlingService wasNot called }
            assertThat(relatertBehandling).isNull()
        }
    }
}
