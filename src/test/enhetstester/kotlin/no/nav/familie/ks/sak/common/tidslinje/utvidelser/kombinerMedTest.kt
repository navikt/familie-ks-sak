import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class kombinerMedTest {

    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val førsteFebruar = LocalDate.of(2022, 2, 1)
    private val sisteDagIFebruar = LocalDate.of(2022, 2, 28)
    private val førsteMars = LocalDate.of(2022, 3, 1)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)
    private val førsteApril = LocalDate.of(2022, 4, 1)
    private val sisteDagIApril = LocalDate.of(2022, 4, 30)

    /**
     * a = |111-|
     * b = |-2-2|
     * (a ?: 0) + (b ?: 0) = |1312|
     **/
    @Test
    fun `kombinerMed - Skal kombinere overlappende verdier på tidslinjene`() {
        val tidslinjeA = listOf(Periode(1, førsteJanuar, sisteDagIMars)).tilTidslinje()
        val tidslinjeB =
            listOf(Periode(2, førsteFebruar, sisteDagIFebruar), Periode(2, førsteApril, sisteDagIApril)).tilTidslinje()

        val periode = tidslinjeA.kombinerMed(tidslinjeB) { verdiFraTidslinjeA, verdiFraTidslinjeB ->
            (verdiFraTidslinjeA ?: 0) + (verdiFraTidslinjeB ?: 0)
        }.tilPerioder()

        Assertions.assertEquals(4, periode.size)

        Assertions.assertEquals(førsteJanuar, periode[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, periode[0].tom)
        Assertions.assertEquals(1, periode[0].verdi)

        Assertions.assertEquals(førsteFebruar, periode[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, periode[1].tom)
        Assertions.assertEquals(3, periode[1].verdi)

        Assertions.assertEquals(førsteMars, periode[2].fom)
        Assertions.assertEquals(sisteDagIMars, periode[2].tom)
        Assertions.assertEquals(1, periode[2].verdi)

        Assertions.assertEquals(førsteApril, periode[3].fom)
        Assertions.assertEquals(sisteDagIApril, periode[3].tom)
        Assertions.assertEquals(2, periode[3].verdi)
    }

    /**
     * a = |1--|
     * b = |--2|
     * (a ?: 0) + (b ?: 0) = |102|
     **/
    @Test
    fun `kombinerMed - Skal ikke kombinere verdier som ikke overlapper`() {
        val tidslinjeA = listOf(Periode(1, førsteJanuar, sisteDagIJanuar)).tilTidslinje()
        val tidslinjeB = listOf(Periode(2, førsteMars, sisteDagIMars)).tilTidslinje()

        val periode = tidslinjeA.kombinerMed(tidslinjeB) { verdiFraTidslinjeA, verdiFraTidslinjeB ->
            (verdiFraTidslinjeA ?: 0) + (verdiFraTidslinjeB ?: 0)
        }.tilPerioder()

        Assertions.assertEquals(3, periode.size)

        Assertions.assertEquals(førsteJanuar, periode[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, periode[0].tom)
        Assertions.assertEquals(1, periode[0].verdi)

        Assertions.assertEquals(førsteFebruar, periode[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, periode[1].tom)
        Assertions.assertEquals(0, periode[1].verdi)

        Assertions.assertEquals(førsteMars, periode[2].fom)
        Assertions.assertEquals(sisteDagIMars, periode[2].tom)
        Assertions.assertEquals(2, periode[2].verdi)
    }
}
