package no.nav.familie.ks.sak.common.util

import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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

        assertThat(yearMonth, Is(YearMonth.of(2020, 12)))
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
    fun `LocalDateTime erHverdag() skal returnere true dersom det er en hverdag`() {
        val mandag = LocalDateTime.of(2022, 10, 24, 0, 0)

        assertThat(mandag.erHverdag(), Is(true))
        assertThat(mandag.plusDays(1).erHverdag(), Is(true))
        assertThat(mandag.plusDays(2).erHverdag(), Is(true))
        assertThat(mandag.plusDays(3).erHverdag(), Is(true))
        assertThat(mandag.plusDays(4).erHverdag(), Is(true))
        assertThat(mandag.plusDays(5).erHverdag(), Is(false))
        assertThat(mandag.plusDays(6).erHverdag(), Is(false))
        assertThat(mandag.plusDays(7).erHverdag(), Is(true))
    }

    @ParameterizedTest
    @CsvSource("21:04", "23:02", "00:39", "04:04", "05:59")
    fun `erKlokkenMellom21Og06 skal returnere true dersom klokken er mellom 21 og 06`(localTime: LocalTime) = assertThat(erKlokkenMellom21Og06(localTime), Is(true))

    @ParameterizedTest
    @CsvSource("07:04", "09:15", "12:55", "18:20", "20:59")
    fun `erKlokkenMellom21Og06 skal returnere false dersom klokken ikke er mellom 21 og 06`(localTime: LocalTime) = assertThat(erKlokkenMellom21Og06(localTime), Is(false))

    @Test
    fun `LocalDate erHelligDag skal returnere true for alle helligedager`() {
        assertTrue { LocalDate.of(2022, 1, 1).erHelligdag() }
        assertTrue { LocalDate.of(2022, 5, 1).erHelligdag() }
        assertTrue { LocalDate.of(2022, 5, 17).erHelligdag() }
        assertTrue { LocalDate.of(2022, 12, 25).erHelligdag() }
        assertTrue { LocalDate.of(2022, 12, 26).erHelligdag() }

        assertFalse { LocalDate.of(2022, 11, 28).erHelligdag() }
    }
}
