package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKlagebehandlingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class RelatertBehandlingTest {
    @Nested
    inner class FraKontantstøttebehandling {
        @Test
        fun `skal lage relatert behandling fra kontatstøttebehandling`() {
            // Arrange
            val kontantstøttebehandling =
                lagBehandling(
                    id = 1L,
                    aktivertTidspunkt = LocalDateTime.now(),
                )

            // Act
            val relatertBehandling = RelatertBehandling.fraKontantstøttebehandling(kontantstøttebehandling)

            // Assert
            assertThat(relatertBehandling.id).isEqualTo(kontantstøttebehandling.id.toString())
            assertThat(relatertBehandling.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KONT)
            assertThat(relatertBehandling.vedtattTidspunkt).isEqualTo(kontantstøttebehandling.aktivertTidspunkt)
        }
    }

    @Nested
    inner class FraKlagebehandling {
        @Test
        fun `skal lage relatert behandling fra klagebehandling`() {
            // Arrange
            val klagebehandling =
                lagKlagebehandlingDto(
                    id = UUID.randomUUID(),
                    vedtaksdato = LocalDateTime.now(),
                )

            // Act
            val relatertBehandling = RelatertBehandling.fraKlagebehandling(klagebehandling)

            // Assert
            assertThat(relatertBehandling.id).isEqualTo(klagebehandling.id.toString())
            assertThat(relatertBehandling.fagsystem).isEqualTo(RelatertBehandling.Fagsystem.KLAGE)
            assertThat(relatertBehandling.vedtattTidspunkt).isEqualTo(klagebehandling.vedtaksdato)
        }

        @Test
        fun `skal kaste exception om klagebehandling mangler vedtaksdato`() {
            // Arrange
            val klagebehandling =
                lagKlagebehandlingDto(
                    id = UUID.randomUUID(),
                    vedtaksdato = null,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    RelatertBehandling.fraKlagebehandling(klagebehandling)
                }
            assertThat(exception.message).isEqualTo("Forventer vedtaksdato for klagebehandling ${klagebehandling.id}")
        }
    }
}
