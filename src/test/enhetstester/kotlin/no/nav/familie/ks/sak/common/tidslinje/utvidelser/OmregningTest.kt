package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.tidslinje.TidsEnhet
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class OmregningTest {

    @Test
    fun `kan omgjøre fra uke til dag`() {
        val dato1_start = LocalDate.of(2022, 7, 4)
        val dato1_slutt = LocalDate.of(2022, 7, 10)

        val dato2_start = LocalDate.of(2022, 7, 11)
        val dato2_slutt = LocalDate.of(2022, 7, 17)

        val dato3_start = LocalDate.of(2022, 7, 18)
        val dato3_slutt = LocalDate.of(2022, 7, 24)

        val dato4_start = LocalDate.of(2022, 7, 25)
        val dato4_slutt = LocalDate.of(2022, 7, 31)

        val tmp = listOf(
            TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.WEEKS).toInt() + 1),
            TidslinjePeriode(2, dato2_start.until(dato2_slutt, ChronoUnit.WEEKS).toInt() + 1),
            TidslinjePeriode(3, dato3_start.until(dato3_slutt, ChronoUnit.WEEKS).toInt() + 1),
            TidslinjePeriode(4, dato4_start.until(dato4_slutt, ChronoUnit.WEEKS).toInt() + 1),
        )

        val tidslinje = Tidslinje(dato1_start, tmp, tidsEnhet = TidsEnhet.UKE)

        val correct = listOf(1, 2, 3, 4)

        val tidslinjeDag = tidslinje.konverterTilDag()

        Assertions.assertEquals(correct, tidslinjeDag.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1_start, tidslinjeDag.startsTidspunkt)

        Assertions.assertEquals(dato4_slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan omgjøre fra måned til dag`() {
        val dato1_start = LocalDate.of(2022, 7, 1)
        val dato1_slutt = LocalDate.of(2022, 7, 31)

        val dato2_start = LocalDate.of(2022, 8, 1)
        val dato2_slutt = LocalDate.of(2022, 8, 31)

        val dato3_start = LocalDate.of(2022, 9, 1)
        val dato3_slutt = LocalDate.of(2022, 9, 30)

        val dato4_start = LocalDate.of(2022, 10, 1)
        val dato4_slutt = LocalDate.of(2022, 10, 31)

        val tmp = listOf(
            TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.MONTHS).toInt() + 1),
            TidslinjePeriode(2, dato2_start.until(dato2_slutt, ChronoUnit.MONTHS).toInt() + 1),
            TidslinjePeriode(3, dato3_start.until(dato3_slutt, ChronoUnit.MONTHS).toInt() + 1),
            TidslinjePeriode(4, dato4_start.until(dato4_slutt, ChronoUnit.MONTHS).toInt() + 1),
        )

        val tidslinje = Tidslinje(dato1_start, tmp, tidsEnhet = TidsEnhet.MÅNED)

        val correct = listOf(1, 2, 3, 4)

        val tidslinjeDag = tidslinje.konverterTilDag()

        Assertions.assertEquals(correct, tidslinjeDag.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1_start, tidslinjeDag.startsTidspunkt)

        Assertions.assertEquals(dato4_slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan omgjøre fra år til dag`() {
        val dato1_start = LocalDate.of(2022, 1, 1)
        val dato1_slutt = LocalDate.of(2023, 12, 31)

        val dato2_start = LocalDate.of(2024, 1, 1)
        val dato2_slutt = LocalDate.of(2024, 12, 31)

        val dato3_start = LocalDate.of(2025, 1, 1)
        val dato3_slutt = LocalDate.of(2025, 12, 31)

        val dato4_start = LocalDate.of(2026, 1, 1)
        val dato4_slutt = LocalDate.of(2026, 12, 31)

        val tidslinjePerioder = listOf(
            TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.YEARS).toInt() + 1),
            TidslinjePeriode(2, dato2_start.until(dato2_slutt, ChronoUnit.YEARS).toInt() + 1),
            TidslinjePeriode(3, dato3_start.until(dato3_slutt, ChronoUnit.YEARS).toInt() + 1),
            TidslinjePeriode(4, dato4_start.until(dato4_slutt, ChronoUnit.YEARS).toInt() + 1),
        )

        val tidslinje = Tidslinje(dato1_start, tidslinjePerioder, tidsEnhet = TidsEnhet.ÅR)

        val forventedeVerdier = listOf(1, 2, 3, 4)

        val tidslinjeDag = tidslinje.konverterTilDag()

        Assertions.assertEquals(forventedeVerdier, tidslinjeDag.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1_start.withDayOfYear(1), tidslinjeDag.startsTidspunkt)

        Assertions.assertEquals(dato4_slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }

    @Test
    fun `Riktig start- og slutttidspunkt`() {
        val dato1_start = LocalDate.of(2022, 1, 1)
        val dato1_slutt = LocalDate.of(2023, 12, 31)

        val tmpÅR = listOf(TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.YEARS).toInt() + 1))
        val tmpMåned = listOf(TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.MONTHS).toInt() + 1))
        val tmpUke = listOf(TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.WEEKS).toInt() + 1))
        val tmpDag = listOf(TidslinjePeriode(1, dato1_start.until(dato1_slutt, ChronoUnit.DAYS).toInt() + 1))

        val tidslinjeÅR = Tidslinje(dato1_start, tmpÅR, tidsEnhet = TidsEnhet.ÅR)
        val tidslinjeMåned = Tidslinje(dato1_start, tmpMåned, tidsEnhet = TidsEnhet.MÅNED)
        val tidslinjeUke = Tidslinje(dato1_start, tmpUke, tidsEnhet = TidsEnhet.UKE)
        val tidslinjeDag = Tidslinje(dato1_start, tmpDag, tidsEnhet = TidsEnhet.DAG)

        Assertions.assertEquals(dato1_start, tidslinjeÅR.startsTidspunkt)
        Assertions.assertEquals(dato1_start, tidslinjeMåned.startsTidspunkt)
        Assertions.assertEquals(dato1_start, tidslinjeDag.startsTidspunkt)
        Assertions.assertEquals(LocalDate.of(2021, 12, 27), tidslinjeUke.startsTidspunkt)

        Assertions.assertEquals(dato1_slutt, tidslinjeÅR.kalkulerSluttTidspunkt())
        Assertions.assertEquals(dato1_slutt, tidslinjeMåned.kalkulerSluttTidspunkt())
        Assertions.assertEquals(dato1_slutt, tidslinjeUke.kalkulerSluttTidspunkt())
        Assertions.assertEquals(dato1_slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }
}
