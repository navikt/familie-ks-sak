package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.common.tidslinje.TidsEnhet
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriode
import no.nav.familie.ks.sak.common.tidslinje.Udefinert
import no.nav.familie.ks.sak.common.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SnittOgKlippTest {

    @Test
    fun `kan ta snittet av to tidslinjer med ulik lengde`() {
        val t1 = Tidslinje(
            startsTidspunkt = LocalDate.of(2022, 2, 1),
            perioder = listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(15, 2, false)
            ),
            tidsEnhet = TidsEnhet.MÅNED
        )

        val t2 = Tidslinje(
            startsTidspunkt = LocalDate.of(2022, 2, 1),
            perioder = listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(15, 11, false)
            ),
            tidsEnhet = TidsEnhet.MÅNED
        )

        val resultat = t1.biFunksjonSnitt(t2) { el1, el2 ->
            if (el1 is Udefinert || el2 is Udefinert) Udefinert()
            else Verdi(el1.verdi!! + el2.verdi!!)
        }

        val fasit = mutableListOf(2, 4, 30)

        Assertions.assertEquals(
            fasit,
            resultat.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt på månedsnivå"
        )
        Assertions.assertEquals(resultat.tidsEnhet, TidsEnhet.MÅNED)

        val endDate = LocalDate.of(2022, 2, 1).plusMonths(3)
        Assertions.assertEquals(endDate.withDayOfMonth(endDate.lengthOfMonth()), resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan klippe en tidslinje slik at dem får riktig start og sluttdato`() {
        var tidslinje = Tidslinje(
            startsTidspunkt = LocalDate.of(2022, 2, 1),
            perioder = listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(15, 11, false)
            ),
            tidsEnhet = TidsEnhet.MÅNED
        )

        var startDato = LocalDate.of(2022, 3, 1)
        var sluttDato = LocalDate.of(2022, 4, 30)

        var fasit = mutableListOf(2, 15)

        tidslinje = tidslinje.klipp(startDato, sluttDato)

        Assertions.assertEquals(fasit, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
        Assertions.assertEquals(startDato, tidslinje.startsTidspunkt)
        Assertions.assertEquals(sluttDato, tidslinje.kalkulerSluttTidspunkt())

        tidslinje = Tidslinje(
            startsTidspunkt = LocalDate.of(2022, 2, 1),
            perioder = listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(15, 11, false)
            ),
            tidsEnhet = TidsEnhet.MÅNED
        )

        startDato = LocalDate.of(2022, 2, 3)
        sluttDato = LocalDate.of(2022, 2, 6)

        fasit = mutableListOf(1)

        tidslinje = tidslinje.konverterTilDag().klipp(startDato, sluttDato)

        Assertions.assertEquals(fasit, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
        Assertions.assertEquals(startDato, tidslinje.startsTidspunkt)
        Assertions.assertEquals(sluttDato, tidslinje.kalkulerSluttTidspunkt())
    }
}
