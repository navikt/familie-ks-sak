package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KorrigertVedtakRequestDtoKtTest {
    @Nested
    inner class TilKorrigertVedtak {
        @Test
        fun `Skal sette aktiv til true og overføre resten av felter fra dto til domene objekt`() {
            // Arrange
            val januar2021 = LocalDate.of(2021, 1, 1)

            val korrigertVedtakRequestDto =
                KorrigertVedtakRequestDto(
                    vedtaksdato = januar2021,
                    begrunnelse = "test",
                )

            // Act
            val korrigertVedtak = korrigertVedtakRequestDto.tilKorrigertVedtak(lagBehandling())

            // Assert
            assertThat(korrigertVedtak.vedtaksdato).isEqualTo(januar2021)
            assertThat(korrigertVedtak.begrunnelse).isEqualTo("test")
            assertThat(korrigertVedtak.aktiv).isTrue()
        }
    }

    @Nested
    inner class TilKorrigertVedtakResponsDto {
        @Test
        fun `Skal mappes over til respons dto med lik informasjon som domene objekt`() {
            // Arrange
            val januar2021 = LocalDate.of(2021, 1, 1)

            val korrigertVedtakRequestDto =
                KorrigertVedtak(
                    vedtaksdato = januar2021,
                    begrunnelse = "test",
                    behandling = lagBehandling(),
                    aktiv = true,
                    id = 20,
                )

            // Act
            val korrigertVedtakResponsDto = korrigertVedtakRequestDto.tilKorrigertVedtakResponsDto()

            // Assert
            assertThat(korrigertVedtakResponsDto.vedtaksdato).isEqualTo(januar2021)
            assertThat(korrigertVedtakResponsDto.begrunnelse).isEqualTo("test")
            assertThat(korrigertVedtakResponsDto.aktiv).isTrue()
            assertThat(korrigertVedtakResponsDto.id).isEqualTo(20)
        }
    }
}
