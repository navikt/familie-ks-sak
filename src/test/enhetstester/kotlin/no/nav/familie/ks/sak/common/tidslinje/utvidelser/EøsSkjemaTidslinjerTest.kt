package no.nav.familie.ks.sak.common.tidslinje.utvidelser

import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class EøsSkjemaTidslinjerTest {
    @Test
    fun `skal håndtere to påfølgende perioder i fremtiden, men de komprimeres ikke`() {
        val barn = lagPerson(aktør = randomAktør(), personType = PersonType.BARN)
        val kompetanse1 =
            Kompetanse(
                fom = YearMonth.of(2437, 2),
                tom = YearMonth.of(2438, 6),
                barnAktører = setOf(barn.aktør),
            )
        val kompetanse2 =
            Kompetanse(
                fom = YearMonth.of(2438, 7),
                tom = null,
                barnAktører = setOf(barn.aktør),
            )

        val kompetanseTidslinje = listOf(kompetanse1, kompetanse2).tilTidslinje()
        assertEquals(1, kompetanseTidslinje.tilPerioder().size)

        val periode = kompetanseTidslinje.tilPerioder()[0]
        assertEquals(LocalDate.of(2437, 2, 1), periode.fom)
        assertNull(periode.tom)
    }
}
