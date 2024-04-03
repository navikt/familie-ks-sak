package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.data.lagBeregnetUtbetalingsoppdrag
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate

internal class UtbetalingsoppdragValidatorTest {
    @Test
    fun `valider skal kaste feil dersom utbetalingsoppdrag inneholder perioder og resultat er FORTSATT_INNVILGET`() {
        val utbetalingsoppdrag =
            lagBeregnetUtbetalingsoppdrag(
                lagVedtak(),
                listOf(
                    Utbetalingsperiode(
                        erEndringPåEksisterendePeriode = false,
                        periodeId = 0,
                        datoForVedtak = LocalDate.now(),
                        klassifisering = "KS",
                        vedtakdatoFom = LocalDate.now().minusMonths(3).førsteDagINesteMåned(),
                        vedtakdatoTom = LocalDate.now().sisteDagIMåned(),
                        sats = BigDecimal.valueOf(1000),
                        satsType = Utbetalingsperiode.SatsType.MND,
                        utbetalesTil = "",
                        behandlingId = 0,
                    ),
                ),
            )

        val funksjonellFeil =
            assertThrows<FunksjonellFeil> {
                utbetalingsoppdrag.valider(
                    behandlingsresultat = Behandlingsresultat.FORTSATT_INNVILGET,
                )
            }

        assertThat(funksjonellFeil.message).isEqualTo("Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX")
    }

    @Test
    fun `valider ikke skal kaste feil dersom utbetalingsoppdrag ikke inneholder perioder og resultat er FORTSATT_INNVILGET`() {
        val utbetalingsoppdrag =
            lagBeregnetUtbetalingsoppdrag(
                lagVedtak(),
                emptyList(),
            )

        assertDoesNotThrow {
            utbetalingsoppdrag.valider(
                behandlingsresultat = Behandlingsresultat.FORTSATT_INNVILGET,
            )
        }
    }

    @ParameterizedTest
    @EnumSource(value = Behandlingsresultat::class, names = ["FORTSATT_INNVILGET"], mode = EnumSource.Mode.EXCLUDE)
    fun `valider skal ikke kaste feil dersom utbetalingsoppdrag inneholder perioder og resultat ikke er FORTSATT_INNVILGET`(behandlingsresultat: Behandlingsresultat) {
        val utbetalingsoppdrag =
            lagBeregnetUtbetalingsoppdrag(
                lagVedtak(),
                listOf(
                    Utbetalingsperiode(
                        erEndringPåEksisterendePeriode = false,
                        periodeId = 0,
                        datoForVedtak = LocalDate.now(),
                        klassifisering = "KS",
                        vedtakdatoFom = LocalDate.now().minusMonths(3).førsteDagINesteMåned(),
                        vedtakdatoTom = LocalDate.now().sisteDagIMåned(),
                        sats = BigDecimal.valueOf(1000),
                        satsType = Utbetalingsperiode.SatsType.MND,
                        utbetalesTil = "",
                        behandlingId = 0,
                    ),
                ),
            )

        assertDoesNotThrow {
            utbetalingsoppdrag.valider(
                behandlingsresultat = behandlingsresultat,
            )
        }
    }
}
