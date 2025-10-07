package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagEksternBehandlingRelasjon
import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class RelatertBehandlingTest {
    @Nested
    inner class FraKontantstøttebehandling {
        @Test
        fun `skal opprette fra kontantstøttebehandling`() {
            // Arrange
            val behandling = lagBehandling()

            // Act
            val relatertBehandling = RelatertBehandling.fraKontantstøttebehandling(behandling)

            // Assert
            assertThat(relatertBehandling.id).isEqualTo(behandling.id.toString())
            assertThat(relatertBehandling.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KS)
        }
    }

    @Nested
    inner class FraEksternBehandlingRelasjon {
        @ParameterizedTest
        @EnumSource(value = EksternBehandlingRelasjon.Fagsystem::class)
        fun `skal opprette fra ekstern behandling relasjon`(
            fagsystem: EksternBehandlingRelasjon.Fagsystem,
        ) {
            // Arrange
            val eksternBehandlingRelasjon =
                lagEksternBehandlingRelasjon(
                    eksternBehandlingFagsystem = fagsystem,
                )

            // Act
            val relatertBehandling = RelatertBehandling.fraEksternBehandlingRelasjon(eksternBehandlingRelasjon)

            // Assert
            assertThat(relatertBehandling.id).isEqualTo(eksternBehandlingRelasjon.eksternBehandlingId)
            assertThat(relatertBehandling.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.valueOf(fagsystem.name))
        }
    }
}
