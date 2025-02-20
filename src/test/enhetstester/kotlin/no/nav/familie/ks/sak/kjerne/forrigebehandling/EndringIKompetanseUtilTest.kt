package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class EndringIKompetanseUtilTest {
    private val barn1Aktør = randomAktør()
    private val jan22 = YearMonth.of(2022, 1)
    private val mai22 = YearMonth.of(2022, 5)

    @Nested
    inner class UtledEndringstidspunktForEndretUtbetalingAndelTest {
        @Test
        fun `Skal ikke returnere noe endringstidspunkt hvis ingen ting har endret seg siden sist`() {
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

            val endringstidspunkt =
                EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
                    nåværendeKompetanser = listOf(nåværendeKompetanse),
                    forrigeKompetanser = listOf(forrigeKompetanse),
                )

            Assertions.assertNull(endringstidspunkt)
        }

        @Test
        fun `Skal returnere riktig endringstidspunkt når søkers aktivitetsland endrer seg`() {
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

            val endringstidspunkt =
                EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
                    nåværendeKompetanser =
                        listOf(
                            nåværendeKompetanse,
                        ),
                    forrigeKompetanser = listOf(forrigeKompetanse),
                )

            assertThat(jan22).isEqualTo(endringstidspunkt)
        }

        @Test
        fun `Endringtidspunkt skal ikke endre på seg når det settes ekstra kompetanseperiode`() {
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
                forrigeKompetanse
                    .copy(fom = YearMonth.now().minusMonths(10))
                    .apply { behandlingId = nåværendeBehandling.id }

            val endringstidspunkt =
                EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
                    nåværendeKompetanser =
                        listOf(
                            nåværendeKompetanse,
                        ),
                    forrigeKompetanser = listOf(forrigeKompetanse),
                )

            Assertions.assertNull(endringstidspunkt)
        }

        @Test
        fun `Endringstidspunkt skal ikke endre på seg når man fyller ut kompetanse som tidligere ikke var utfylt (pga migrering+ evt autovedtak)`() {
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

            val endringstidspunkt =
                EndringIKompetanseUtil.utledEndringstidspunktForKompetanse(
                    nåværendeKompetanser =
                        listOf(
                            nåværendeKompetanse,
                        ),
                    forrigeKompetanser = listOf(forrigeKompetanse),
                )

            Assertions.assertNull(endringstidspunkt)
        }
    }

    @Nested
    inner class LagEndringIKompetanseForPersonTidslinjeTest {
        @Test
        fun `Skal ikke returnere noen endrede perioder når ingenting endrer seg`() {
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
                EndringIKompetanseUtil
                    .lagEndringIKompetanseForPersonTidslinje(
                        nåværendeKompetanserForPerson = listOf(nåværendeKompetanse),
                        forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            Assertions.assertTrue(perioderMedEndring.isEmpty())
        }

        @Test
        fun `Skal returnere endret periode når søkers aktivitetsland endrer seg`() {
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
                EndringIKompetanseUtil
                    .lagEndringIKompetanseForPersonTidslinje(
                        nåværendeKompetanserForPerson =
                            listOf(
                                nåværendeKompetanse,
                            ),
                        forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            assertThat(1).isEqualTo(perioderMedEndring.size)
            assertThat(jan22.førsteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().fom)
            assertThat(mai22.sisteDagIInneværendeMåned()).isEqualTo(perioderMedEndring.single().tom)
        }

        @Test
        fun `Skal ikke lage endret periode når det kun blir lagt på en ekstra kompetanseperiode`() {
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
                forrigeKompetanse
                    .copy(fom = YearMonth.now().minusMonths(10))
                    .apply { behandlingId = nåværendeBehandling.id }

            val perioderMedEndring =
                EndringIKompetanseUtil
                    .lagEndringIKompetanseForPersonTidslinje(
                        nåværendeKompetanserForPerson =
                            listOf(
                                nåværendeKompetanse,
                            ),
                        forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            Assertions.assertTrue(perioderMedEndring.isEmpty())
        }

        @Test
        fun `Skal ikke lage endret periode når forrige kompetanse ikke er utfylt (pga migrering+ evt autovedtak)`() {
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
                EndringIKompetanseUtil
                    .lagEndringIKompetanseForPersonTidslinje(
                        nåværendeKompetanserForPerson =
                            listOf(
                                nåværendeKompetanse,
                            ),
                        forrigeKompetanserForPerson = listOf(forrigeKompetanse),
                    ).tilPerioder()
                    .filter { it.verdi == true }

            Assertions.assertTrue(perioderMedEndring.isEmpty())
        }
    }
}
