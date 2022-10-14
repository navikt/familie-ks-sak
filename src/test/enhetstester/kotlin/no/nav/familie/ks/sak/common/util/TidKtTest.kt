package no.nav.familie.ks.sak.common.util

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

internal class TidKtTest {

    @Test
    fun `LocalDate tilddMMyy() skal formatere dato til ddMMyy format`() {
        val localDate = LocalDate.of(2020, 12, 16)

        assertThat(localDate.tilddMMyy(), Is("161220"))
    }

    @Test
    fun `LocalDate tilyyyyMMdd() skal formatere dato til yyyy-MM-dd format`() {
        val localDate = LocalDate.of(2020, 12, 16)

        assertThat(localDate.tilyyyyMMdd(), Is("2020-12-16"))
    }

    @Test
    fun `LocalDate tilKortString() skal formatere dato til ddMMyy format`() {
        val localDate = LocalDate.of(2020, 12, 16)

        assertThat(localDate.tilKortString(), Is("16.12.20"))
    }

    @Test
    fun `LocalDate tilDagMånedÅr() skal formatere dato til d MMMM yyyy format`() {
        val localDate = LocalDate.of(2020, 12, 16)

        assertThat(localDate.tilDagMånedÅr(), Is("16. desember 2020"))
    }

    @Test
    fun `LocalDate tilMånedÅr() skal formatere dato til MMMM yyyy format`() {
        val localDate = LocalDate.of(2020, 12, 16)

        assertThat(localDate.tilMånedÅr(), Is("desember 2020"))
    }

    @Test
    fun `LocalDate tilYearMonth() skal konverte LocalDate til YearMonth`() {
        val localDate = LocalDate.of(2020, 12, 16)
        val yearMonth = localDate.tilYearMonth()

        assertThat(yearMonth, Is(YearMonth.of(2020,12)))
    }

    @Test
    fun `LocalDate sisteDagIMåned() skal sette dagen til å være siste dagi måned`() {
        val localDate = LocalDate.of(2020, 12, 16)

        assertThat(localDate.sisteDagIMåned(), Is(LocalDate.of(2020, 12, 31)))
    }

    @Test
    fun `YearMonth tilKortString() skal formatere dato til MM yy format`() {
        val yearMonth = YearMonth.of(2020, 12)

        assertThat(yearMonth.tilKortString(), Is("12.20"))
    }

    @Test
    fun `YearMonth tilMånedÅr() skal formatere dato til MMMM yyyy format`() {
        val yearMonth = YearMonth.of(2020, 12)

        assertThat(yearMonth.tilMånedÅr(), Is("desember 2020"))
    }
}
