package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.tilPeriode
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

object EndringIEndretUtbetalingAndelUtil {
    fun utledEndringstidspunktForEndretUtbetalingAndel(
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
    ): YearMonth? {
        val allePersoner = (nåværendeEndretAndeler.mapNotNull { it.person?.aktør } + forrigeEndretAndeler.mapNotNull { it.person?.aktør }).distinct()

        val endretUtbetalingTidslinje =
            allePersoner
                .map { aktør ->
                    lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        nåværendeEndretAndelerForPerson = nåværendeEndretAndeler.filter { it.person?.aktør == aktør },
                        forrigeEndretAndelerForPerson = forrigeEndretAndeler.filter { it.person?.aktør == aktør },
                    )
                }.kombiner { finnesMinstEnEndringIPeriode(it) }

        return endretUtbetalingTidslinje.tilFørsteEndringstidspunkt()
    }

    fun lagEndringIEndretUbetalingAndelPerPersonTidslinje(
        nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
        forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeEndretAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()
        val forrigeTidslinje = forrigeEndretAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val erFulltidsplassIBarnehageAugust2024MedEksplisittAvslag = nåværende?.årsak == Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 && nåværende.erEksplisittAvslagPåSøknad == true
                (
                    nåværende?.årsak != forrige?.årsak &&
                        !erFulltidsplassIBarnehageAugust2024MedEksplisittAvslag
                )
            }

        return endringerTidslinje
    }

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }
}
