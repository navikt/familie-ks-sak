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
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
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
            val behandling = lagBehandling()

            every { unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false) } returns false

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @Test
        fun `skal utlede relatert behandling når klagebehandlingen er siste vedtatte behandling og den innsendte behandling er en revurdering med årsak klage`() {
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

            val kontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.KLAGE,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val klagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurderingKlage.fagsak.id) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns klagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(klagebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(klagebehandling.vedtaksdato)
        }

        @Test
        fun `skal utlede relatert behandling når klagebehandlingen er siste vedtatte behandling men den innsendte behandling ikke er en revurdering med årsak klage`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val kontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val klagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurderingKlage.fagsak.id) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns klagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(kontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(kontantstøttebehandling.aktivertTidspunkt)
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
            val nåtidspunkt = LocalDateTime.now()

            val revurderingKlage =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.KLAGE,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val kontantstøttebehandling =
                lagBehandling(
                    type = behandlingType,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val klagebehandling =
                lagKlagebehandlingDto(
                    vedtaksdato = nåtidspunkt.minusSeconds(3),
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurderingKlage.fagsak.id) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(revurderingKlage.fagsak.id) } returns klagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurderingKlage)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(kontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(kontantstøttebehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal ikke utlede relatert behandling når ingen behandlinger er vedtatt`() {
            // Arrange
            val behandling = lagBehandling()

            every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
            every { klageService.hentSisteVedtatteKlagebehandling(behandling.fagsak.id) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(behandling)

            // Assert
            assertThat(relatertBehandling).isNull()
        }

        @Test
        fun `skal ikke utlede relatert behandling når kun førstegangsbehandling for kontantstøtte er vedtatt`() {
            // Arrange
            val nåtidspunkt = LocalDateTime.now()

            val revurdering =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    opprettetÅrsak = BehandlingÅrsak.KLAGE,
                    aktivertTidspunkt = nåtidspunkt,
                    status = BehandlingStatus.UTREDES,
                    resultat = Behandlingsresultat.IKKE_VURDERT,
                )

            val kontantstøttebehandling =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingService.hentSisteBehandlingSomErVedtatt(revurdering.fagsak.id) } returns kontantstøttebehandling
            every { klageService.hentSisteVedtatteKlagebehandling(revurdering.fagsak.id) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(revurdering)

            // Assert
            assertThat(relatertBehandling).isNull()
        }
    }
}
