package no.nav.familie.ks.sak.statistikk.saksstatistikk

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKlagebehandlingDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            val behandling = lagBehandling()

            every { unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false) } returns false

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @Test
        fun `skal kaste feil om ingen vedtatt klagebehandling finnes når den innsendte behandling er en revurdering med årsak klage`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.KLAGE,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)
                }
            assertThat(exception.message).isEqualTo("Forventer en vedtatt klagebehandling for behandling ${revurderingKlage.behandlingId}")
            verify { behandlingService wasNot called }
        }

        @Test
        fun `skal kaste feil om ingen vedtatt klagebehandling finnes når den innsendte behandling er en revurdering med årsak iverksette ka vedtak`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns null

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)
                }
            assertThat(exception.message).isEqualTo("Forventer en vedtatt klagebehandling for behandling ${revurderingKlage.behandlingId}")
            verify { behandlingService wasNot called }
        }

        @Test
        fun `skal utlede relatert behandling med klagebehandlingen som siste vedtatte behandling når den innsendte behandling er en revurdering med årsak klage`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.KLAGE,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteKlagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns sisteVedtatteKlagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            verify { behandlingService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKlagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKlagebehandling.vedtaksdato)
        }

        @Test
        fun `skal utlede relatert behandling med klagebehandlingen som siste vedtatte behandling når den innsendte behandling er en revurdering med årsak iverksetter ka vedtak`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingIverksetteKaVedtak =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteKlagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { klageService.hentSisteVedtatteKlagebehandling(revurderingIverksetteKaVedtak.fagsak.id) } returns sisteVedtatteKlagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingIverksetteKaVedtak)

            // Assert
            verify { behandlingService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKlagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKlagebehandling.vedtaksdato)
        }

        @Test
        fun `skal utlede relatert behandling med kontantstøttebehandlingen som siste vedtatte behandling når den innsendte behandling ikke er en revurdering med årsak klage eller iverksetter ka vedtak`() {
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

            val sisteVedtatteKontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns sisteVedtatteKontantstøttebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKontantstøttebehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal utlede relatert behandling med kontantstøttebehandlingen som siste vedtatte behandling når den innsendte behandling ikke er en revurdering`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val behandling =
                lagBehandling(
                    type = BehandlingType.TEKNISK_ENDRING,
                    opprettetÅrsak = BehandlingÅrsak.TEKNISK_ENDRING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteKontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.TEKNISK_ENDRING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns sisteVedtatteKontantstøttebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling?.id).isEqualTo(sisteVedtatteKontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(sisteVedtatteKontantstøttebehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal ikke utlede relatert behandling når ingen kontantstøttebehandling er vedtatt og den innsendte behandling ikke er en revurdering med årsak klage eller iverksette ka vedtak`() {
            // Arrange
            val revurdering =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTidspunkt = LocalDateTime.now(),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling).isNull()
        }

        @Test
        fun `skal ikke utlede relatert behandling når kun en førstegangsbehandling for kontantstøtte er vedtatt`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTidspunkt = nåtidspunkt,
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val sisteVedtatteKontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns sisteVedtatteKontantstøttebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            verify { klageService wasNot called }
            assertThat(relatertBehandling).isNull()
        }
    }
}
