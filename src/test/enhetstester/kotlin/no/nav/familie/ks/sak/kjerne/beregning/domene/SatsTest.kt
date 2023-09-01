package no.nav.familie.ks.sak.kjerne.beregning.domene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

internal class SatsTest {

    private val stønadFom = YearMonth.now().minusMonths(5)
    private val stønadTom = YearMonth.now()
    private val satsBeløp = maksBeløp()

    @Test
    fun `hentGyldigSatsFor skal utlede 100 prosent sats når barn ikke har fått barnehageplass`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = null,
            erDeltBosted = false,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )

        assertSatsPeriode(BigDecimal(100), satsPeriode)
    }

    @Test
    fun `hentGyldigSatsFor skal utlede 80 prosent sats når barn har fått barnehageplass i 8 timer`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = BigDecimal(8.99),
            erDeltBosted = false,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )
        assertSatsPeriode(BigDecimal(80), satsPeriode)
    }

    @Test
    fun `hentGyldigSatsFor skal utlede 60 prosent sats når barn har fått barnehageplass mindre enn 16 timer`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = BigDecimal(13.55),
            erDeltBosted = false,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )
        assertSatsPeriode(BigDecimal(60), satsPeriode)
    }

    @Test
    fun `hentGyldigSatsFor skal utlede 40 prosent sats når barn har fått barnehageplass mindre enn 24 timer`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = BigDecimal(22),
            erDeltBosted = false,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )
        assertSatsPeriode(BigDecimal(40), satsPeriode)
    }

    @Test
    fun `hentGyldigSatsFor skal utlede 20 prosent sats når barn har fått barnehageplass i 32 timer`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = BigDecimal(32.85),
            erDeltBosted = false,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )
        assertSatsPeriode(BigDecimal(20), satsPeriode)
    }

    @Test
    fun `hentGyldigSatsFor skal utlede 0 prosent sats når barn har fått barnehageplass mer enn 32 timer`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = BigDecimal(35),
            erDeltBosted = false,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )
        assertSatsPeriode(BigDecimal(0), satsPeriode)
    }

    @Test
    fun `hentGyldigSatsFor skal utlede 50 prosent sats når barn har delt bosted`() {
        val satsPeriode = hentGyldigSatsFor(
            antallTimer = null,
            erDeltBosted = true,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
        )
        assertSatsPeriode(BigDecimal(50), satsPeriode)
    }

    private fun assertSatsPeriode(prosent: BigDecimal?, satsPeriode: SatsPeriode) {
        assertEquals(satsBeløp, satsPeriode.sats)
        assertEquals(prosent, satsPeriode.prosent)
        assertEquals(stønadFom, satsPeriode.fom)
        assertEquals(stønadTom, satsPeriode.tom)
    }
}
