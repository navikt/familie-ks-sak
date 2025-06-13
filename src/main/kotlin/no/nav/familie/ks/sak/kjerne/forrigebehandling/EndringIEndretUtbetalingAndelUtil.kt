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
        val nåværendeAktører = nåværendeEndretAndeler.flatMap { it.personer.map { person -> person.aktør } }.distinct()
        val forrigeAktører = forrigeEndretAndeler.flatMap { it.personer.map { person -> person.aktør } }.distinct()
        val alleAktører = (nåværendeAktører + forrigeAktører).distinct()

        val endringIEndretUtbetalingTidslinjer =
            alleAktører
                .map { aktør ->
                    lagEndringIEndretUbetalingAndelPerPersonTidslinje(
                        nåværendeEndretAndelerForPerson = nåværendeEndretAndeler.filter { it.personer.any { person -> person.aktør == aktør } },
                        forrigeEndretAndelerForPerson = forrigeEndretAndeler.filter { it.personer.any { person -> person.aktør == aktør } },
                    )
                }.kombiner { finnesMinstEnEndringIPeriode(it) }

        return endringIEndretUtbetalingTidslinjer.tilFørsteEndringstidspunkt()
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
