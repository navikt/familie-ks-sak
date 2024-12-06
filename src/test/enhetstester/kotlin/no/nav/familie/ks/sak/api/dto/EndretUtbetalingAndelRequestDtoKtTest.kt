package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EndretUtbetalingAndelRequestDtoKtTest {
    @Nested
    inner class MapTilBegrunnelserTest {
        @Test
        fun `skal returnere en tom liste med begrunnelser om erEksplisittAvslagPåSøknad er false`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    erEksplisittAvslagPåSøknad = false,
                )

            // Act
            val begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()

            // Assert
            assertThat(begrunnelser).isEmpty()
        }

        @Test
        fun `skal returnere AVSLAG_BARNEHAGEPLASS_AUGUST_2024 begrunnelse for årsak DELT_BOSTED`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    erEksplisittAvslagPåSøknad = true,
                    årsak = Årsak.DELT_BOSTED,
                )

            // Act
            val begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()

            // Assert
            assertThat(begrunnelser).containsOnly(NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE)
        }

        @Test
        fun `skal returnere AVSLAG_BARNEHAGEPLASS_AUGUST_2024 begrunnelse for årsak ENDRE_MOTTAKER`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    erEksplisittAvslagPåSøknad = true,
                    årsak = Årsak.ENDRE_MOTTAKER,
                )

            // Act
            val begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()

            // Assert
            assertThat(begrunnelser).containsOnly(NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE)
        }

        @Test
        fun `skal returnere brukersatt begrunnelse for årsak ALLEREDE_UTBETALT`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    erEksplisittAvslagPåSøknad = true,
                    årsak = Årsak.ALLEREDE_UTBETALT,
                    begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER),
                )

            // Act
            val begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()

            // Assert
            assertThat(begrunnelser).containsOnly(NasjonalEllerFellesBegrunnelse.AVSLAG_ENDRINGSPERIODE_ALLEREDE_UTBETALT_ANNEN_FORELDER)
        }

        @Test
        fun `skal returnere AVSLAG_BARNEHAGEPLASS_AUGUST_2024 begrunnelse for årsak ETTERBETALING_3MND`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    erEksplisittAvslagPåSøknad = true,
                    årsak = Årsak.ETTERBETALING_3MND,
                )

            // Act
            val begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()

            // Assert
            assertThat(begrunnelser).containsOnly(NasjonalEllerFellesBegrunnelse.AVSLAG_SØKT_FOR_SENT_ENDRINGSPERIODE)
        }

        @Test
        fun `skal returnere AVSLAG_BARNEHAGEPLASS_AUGUST_2024 begrunnelse for årsak FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024`() {
            // Arrange
            val endretUtbetalingAndelRequestDto =
                lagEndretUtbetalingAndelRequestDto(
                    erEksplisittAvslagPåSøknad = true,
                    årsak = Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                )

            // Act
            val begrunnelser = endretUtbetalingAndelRequestDto.mapTilBegrunnelser()

            // Assert
            assertThat(begrunnelser).containsOnly(NasjonalEllerFellesBegrunnelse.AVSLAG_FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024)
        }
    }
}
