package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

internal class YtelsePersonUtilsTest {
    @Test
    fun `validerYtelsePersoner skal kaste feil hvis resultaten til en av ytelse personene ikke er vurdert`() {
        val exception =
            assertThrows<Feil> {
                YtelsePersonUtils.validerYtelsePersoner(
                    listOf(
                        lagYtelsePerson(setOf(YtelsePersonResultat.INNVILGET)),
                        lagYtelsePerson(setOf(YtelsePersonResultat.IKKE_VURDERT)),
                    ),
                )
            }
        assertEquals("Minst én ytelseperson er ikke vurdert", exception.message)
    }

    @Test
    fun `validerYtelsePersoner skal kaste feil hvis en av ytelse personene ikke får ytelse slutt`() {
        val exception =
            assertThrows<Feil> {
                YtelsePersonUtils.validerYtelsePersoner(
                    listOf(
                        lagYtelsePerson(setOf(YtelsePersonResultat.INNVILGET)),
                        lagYtelsePerson(resultater = setOf(YtelsePersonResultat.INNVILGET), ytelseSlutt = null),
                    ),
                )
            }
        assertEquals("YtelseSlutt er ikke satt ved utledning av behandlingsresultat", exception.message)
    }

    @Test
    fun `validerYtelsePersoner skal ikke kaste feil ved INNVILGET og OPPHØRT`() {
        assertDoesNotThrow {
            YtelsePersonUtils.validerYtelsePersoner(
                listOf(
                    lagYtelsePerson(setOf(YtelsePersonResultat.INNVILGET)),
                    lagYtelsePerson(
                        resultater = setOf(YtelsePersonResultat.OPPHØRT),
                        ytelseSlutt = YearMonth.now(),
                    ),
                ),
            )
        }
    }

    private fun lagYtelsePerson(
        resultater: Set<YtelsePersonResultat>,
        ytelseSlutt: YearMonth? = YearMonth.now().plusMonths(3),
        erDetFramtidigOpphørPåBarnehagevilkåret: Boolean = false,
    ) = YtelsePerson(
        aktør = randomAktør(),
        ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
        kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
        resultater = resultater,
        ytelseSlutt = ytelseSlutt,
        erDetFramtidigOpphørPåBarnehagevilkåret = erDetFramtidigOpphørPåBarnehagevilkåret,
    )
}
