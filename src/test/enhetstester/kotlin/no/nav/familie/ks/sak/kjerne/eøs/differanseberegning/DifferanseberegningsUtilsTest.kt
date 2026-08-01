package no.nav.familie.ks.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall.KVARTALSVIS
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall.MÅNEDLIG
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall.UKENTLIG
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall.ÅRLIG
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.KronerPerValutaenhet
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Valutabeløp
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.konverterBeløpTilMånedlig
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.MathContext
import java.time.YearMonth

class DifferanseberegningsUtilsTest {
    @Test
    fun `Skal multiplisere valutabeløp med valutakurs`() {
        // Arrange
        val valutabeløp = 1200.i("EUR")
        val kurs = 9.731.kronerPer("EUR")

        // Assert
        assertThat((valutabeløp * kurs)?.round(MathContext(5))).isEqualTo(11_677.toBigDecimal())
    }

    @Test
    fun `Skal ikke multiplisere valutabeløp med valutakurs når valuta er forskjellig, men returnere null`() {
        // Arrange
        val valutabeløp = 1200.i("EUR")
        val kurs = 9.73.kronerPer("DKK")

        // Assert
        assertThat(valutabeløp * kurs).isNull()
    }

    @Test
    fun `Skal konvertere årlig utenlandsk periodebeløp til månedlig`() {
        // Arrange
        val månedligValutabeløp =
            1200
                .i("EUR")
                .somUtenlandskPeriodebeløp(ÅRLIG)
                .tilMånedligValutabeløp()

        // Assert
        assertThat(månedligValutabeløp).isEqualTo(100.i("EUR"))
    }

    @Test
    fun `Skal konvertere kvartalsvis utenlandsk periodebeløp til månedlig`() {
        // Arrange
        val månedligValutabeløp =
            300
                .i("EUR")
                .somUtenlandskPeriodebeløp(KVARTALSVIS)
                .tilMånedligValutabeløp()

        // Assert
        assertThat(månedligValutabeløp).isEqualTo(100.i("EUR"))
    }

    @Test
    fun `Månedlig utenlandsk periodebeløp skal ikke endres`() {
        // Arrange
        val månedligValutabeløp =
            100
                .i("EUR")
                .somUtenlandskPeriodebeløp(MÅNEDLIG)
                .tilMånedligValutabeløp()

        // Assert
        assertThat(månedligValutabeløp).isEqualTo(100.i("EUR"))
    }

    @Test
    fun `Skal konvertere ukentlig utenlandsk periodebeløp til månedlig`() {
        // Arrange
        val månedligValutabeløp =
            25
                .i("EUR")
                .somUtenlandskPeriodebeløp(UKENTLIG)
                .tilMånedligValutabeløp()

        // Assert
        assertThat(månedligValutabeløp).isEqualTo(108.75.i("EUR"))
    }

    @Test
    fun `Skal ha presisjon i kronekonverteringen til norske kroner`() {
        // Arrange
        val månedligValutabeløp =
            0.0123767453453
                .i("EUR")
                .somUtenlandskPeriodebeløp(ÅRLIG)
                .tilMånedligValutabeløp()

        // Assert
        assertThat(månedligValutabeløp).isEqualTo(0.0010313954.i("EUR"))
    }

    @Test
    fun `Skal håndtere gjentakende endring og differanseberegning på andel tilkjent ytelse`() {
        // Arrange
        val aty1 =
            lagAndelTilkjentYtelse(beløp = 50).oppdaterDifferanseberegning(
                100.toBigDecimal(),
            )

        // Act & Assert
        assertThat(aty1?.kalkulertUtbetalingsbeløp).isEqualTo(0)
        assertThat(aty1?.differanseberegnetPeriodebeløp).isEqualTo(-50)
        assertThat(aty1?.nasjonaltPeriodebeløp).isEqualTo(50)

        val aty2 =
            aty1?.copy(nasjonaltPeriodebeløp = 1).oppdaterDifferanseberegning(
                75.toBigDecimal(),
            )

        assertThat(aty2?.kalkulertUtbetalingsbeløp).isEqualTo(0)
        assertThat(aty2?.differanseberegnetPeriodebeløp).isEqualTo(-74)
        assertThat(aty2?.nasjonaltPeriodebeløp).isEqualTo(1)

        val aty3 =
            aty2?.copy(nasjonaltPeriodebeløp = 250).oppdaterDifferanseberegning(
                75.toBigDecimal(),
            )

        assertThat(aty3?.kalkulertUtbetalingsbeløp).isEqualTo(175)
        assertThat(aty3?.differanseberegnetPeriodebeløp).isEqualTo(175)
        assertThat(aty3?.nasjonaltPeriodebeløp).isEqualTo(250)
    }

    @Test
    fun `Skal fjerne desimaler i utenlandskperiodebeløp, effektivt øke den norske ytelsen med inntil én krone`() {
        // Arrange
        val aty1 =
            lagAndelTilkjentYtelse(beløp = 50).oppdaterDifferanseberegning(
                100.987654.toBigDecimal(),
            ) // Blir til rundet til 100

        // Assert
        assertThat(aty1?.kalkulertUtbetalingsbeløp).isEqualTo(0)
        assertThat(aty1?.differanseberegnetPeriodebeløp).isEqualTo(-50)
        assertThat(aty1?.nasjonaltPeriodebeløp).isEqualTo(50)
    }

    @Test
    fun `Skal beholde originalt nasjonaltPeriodebeløp når vi oppdatererDifferanseberegning gjentatte ganger`() {
        // Arrange
        var aty1 =
            lagAndelTilkjentYtelse(beløp = 50).oppdaterDifferanseberegning(
                100.987654.toBigDecimal(),
            )

        // Act & Assert
        assertThat(aty1?.kalkulertUtbetalingsbeløp).isEqualTo(0)
        aty1 = aty1.oppdaterDifferanseberegning(13.6.toBigDecimal())
        assertThat(aty1?.kalkulertUtbetalingsbeløp).isEqualTo(37)
        aty1 = aty1.oppdaterDifferanseberegning(49.2.toBigDecimal())
        assertThat(aty1?.kalkulertUtbetalingsbeløp).isEqualTo(1)
    }
}

fun lagAndelTilkjentYtelse(beløp: Int) =
    lagAndelTilkjentYtelse(
        fom = YearMonth.now(),
        tom = YearMonth.now().plusYears(1),
        beløp = beløp,
    )

fun Double.kronerPer(valuta: String) =
    KronerPerValutaenhet(
        valutakode = valuta,
        kronerPerValutaenhet = this.toBigDecimal(),
    )

fun Double.i(valuta: String) = Valutabeløp(this.toBigDecimal(), valuta)

fun Int.i(valuta: String) = Valutabeløp(this.toBigDecimal(), valuta)

fun Valutabeløp.somUtenlandskPeriodebeløp(intervall: Intervall): UtenlandskPeriodebeløp =
    UtenlandskPeriodebeløp(
        fom = null,
        tom = null,
        beløp = this.beløp,
        valutakode = this.valutakode,
        intervall = intervall,
        kalkulertMånedligBeløp = intervall.konverterBeløpTilMånedlig(this.beløp),
    )
