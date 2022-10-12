package no.nav.familie.ks.sak.kjerne.tidslinje

import no.nav.familie.ks.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ks.sak.kjerne.tidslinje.tid.rangeTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

class TidspunktClosedRangeTest {
    @Test
    fun testTidsromMedMåneder() {
        val fom = Tidspunkt.uendeligLengeSiden(YearMonth.of(2020, 1))
        val tom = Tidspunkt.uendeligLengeTil(YearMonth.of(2020, 10))
        val tidsrom = fom..tom

        assertThat(10, Is(tidsrom.count()))
        assertThat(fom, Is(tidsrom.first()))
        assertThat(tom, Is(tidsrom.last()))
    }

    @Test
    fun testTidsromMedDager() {
        val fom = Tidspunkt.uendeligLengeSiden(LocalDate.of(2020, 1, 1))
        val tom = Tidspunkt.uendeligLengeTil(LocalDate.of(2020, 10, 31))
        val tidsrom = fom..tom

        assertThat(305, Is(tidsrom.count()))
        assertThat(fom, Is(tidsrom.first()))
        assertThat(tom, Is(tidsrom.last()))
    }
}
