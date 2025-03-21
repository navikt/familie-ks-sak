package no.nav.familie.ks.sak.statistikk.saksstatistikk

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKlagebehandlingDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.klage.KlagebehandlingHenter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

class RelatertBehandlingUtlederTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val klagebehandlingHenter = mockk<KlagebehandlingHenter>()
    private val unleashService = mockk<UnleashNextMedContextService>()
    private val relatertBehandlingUtleder =
        RelatertBehandlingUtleder(
            behandlingRepository = behandlingRepository,
            klagebehandlingHenter = klagebehandlingHenter,
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

            every { behandlingRepository.finnBehandlinger(fagsakId) } returns listOf(kontantstøttebehandling)
            every { klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId) } returns klagebehandling

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

            every { behandlingRepository.finnBehandlinger(fagsakId) } returns listOf(kontantstøttebehandling)
            every { klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId) } returns klagebehandling

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(kontantstøttebehandling.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KONT)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(kontantstøttebehandling.aktivertTidspunkt)
        }

        @Test
        fun `skal utlede relatert behandling når flere kontantstøttebehandlingen er vedtatt`() {
            // Arrange
            val fagsakId = 1L
            val nåtidspunkt = LocalDateTime.now()

            val kontantstøttebehandling1 =
                lagBehandling(
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(2),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            val kontantstøttebehandling2 =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    aktivertTidspunkt = nåtidspunkt.minusSeconds(1),
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                )

            val kontantstøttebehandling3 =
                lagBehandling(
                    type = BehandlingType.REVURDERING,
                    aktivertTidspunkt = nåtidspunkt,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                )

            every { behandlingRepository.finnBehandlinger(fagsakId) } returns
                listOf(
                    kontantstøttebehandling1,
                    kontantstøttebehandling2,
                    kontantstøttebehandling3,
                )
            every { klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling?.id).isEqualTo(kontantstøttebehandling3.id.toString())
            assertThat(relatertBehandling?.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KONT)
            assertThat(relatertBehandling?.vedtattTidspunkt).isEqualTo(kontantstøttebehandling3.aktivertTidspunkt)
        }

        @Test
        fun `skal ikke utlede relatert behandling når ingen behandlinger er vedtatt`() {
            // Arrange
            val fagsakId = 1L

            every { behandlingRepository.finnBehandlinger(fagsakId) } returns emptyList()
            every { klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId) } returns null

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

            every { behandlingRepository.finnBehandlinger(fagsakId) } returns listOf(kontantstøttebehandling)
            every { klagebehandlingHenter.hentSisteVedtatteKlagebehandling(fagsakId) } returns null

            // Act
            val relatertBehandling = relatertBehandlingUtleder.utledRelatertBehandling(fagsakId)

            // Assert
            assertThat(relatertBehandling).isNull()
        }
    }
}
