package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EndringIKompetanseUtilTest {
    private val barn1Aktør = randomAktør()
    val jan22 = YearMonth.of(2022, 1)
    val mai22 = YearMonth.of(2022, 5)

    @Test
    fun `Endring i kompetanse - skal ikke returnere noen endrede perioder når ingenting endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = jan22,
                tom = mai22,
            )

        val nåværendeKompetanse = forrigeKompetanse.copy().apply { behandlingId = nåværendeBehandling.id }

        val perioderMedEndring =
            EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
                nåværendeKompetanserForPerson = listOf(nåværendeKompetanse),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            ).tilPerioder().filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i kompetanse - skal returnere endret periode når søkers aktivitetsland endrer seg`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = jan22,
                tom = mai22,
            )

        val nåværendeKompetanse =
            forrigeKompetanse.copy(søkersAktivitetsland = "DK").apply { behandlingId = nåværendeBehandling.id }

        val perioderMedEndring =
            EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
                nåværendeKompetanserForPerson =
                    listOf(
                        nåværendeKompetanse,
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            ).tilPerioder().filter { it.verdi == true }

        Assertions.assertEquals(1, perioderMedEndring.size)
        Assertions.assertEquals(jan22.førsteDagIInneværendeMåned(), perioderMedEndring.single().fom)
        Assertions.assertEquals(mai22.sisteDagIInneværendeMåned(), perioderMedEndring.single().tom)
    }

    @Test
    fun `Endring i kompetanse - skal ikke lage endret periode når det kun blir lagt på en ekstra kompetanseperiode`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val nåværendeKompetanse =
            forrigeKompetanse.copy(fom = YearMonth.now().minusMonths(10))
                .apply { behandlingId = nåværendeBehandling.id }

        val perioderMedEndring =
            EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
                nåværendeKompetanserForPerson =
                    listOf(
                        nåværendeKompetanse,
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            ).tilPerioder().filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }

    @Test
    fun `Endring i kompetanse - skal ikke lage endret periode når forrige kompetanse ikke er utfylt (pga migrering+ evt autovedtak)`() {
        val forrigeBehandling = lagBehandling()
        val nåværendeBehandling = lagBehandling()
        val forrigeKompetanse =
            lagKompetanse(
                behandlingId = forrigeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = null,
                søkersAktivitet = null,
                søkersAktivitetsland = null,
                annenForeldersAktivitet = null,
                annenForeldersAktivitetsland = null,
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val nåværendeKompetanse =
            lagKompetanse(
                behandlingId = nåværendeBehandling.id,
                barnAktører = setOf(barn1Aktør),
                barnetsBostedsland = "NO",
                søkersAktivitet = KompetanseAktivitet.ARBEIDER,
                søkersAktivitetsland = "NO",
                annenForeldersAktivitet = KompetanseAktivitet.INAKTIV,
                annenForeldersAktivitetsland = "PO",
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                fom = YearMonth.now().minusMonths(6),
                tom = null,
            )

        val perioderMedEndring =
            EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
                nåværendeKompetanserForPerson =
                    listOf(
                        nåværendeKompetanse,
                    ),
                forrigeKompetanserForPerson = listOf(forrigeKompetanse),
            ).tilPerioder().filter { it.verdi == true }

        Assertions.assertTrue(perioderMedEndring.isEmpty())
    }
}
