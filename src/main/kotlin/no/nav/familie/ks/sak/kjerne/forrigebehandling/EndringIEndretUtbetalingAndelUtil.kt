package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.tilPeriode
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed

object EndringIEndretUtbetalingAndelUtil {
    fun lagEndringIEndretUbetalingAndelPerPersonTidslinje(
        nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
        forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeEndretAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()
        val forrigeTidslinje = forrigeEndretAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val erIkkeFulltidsplassIBarnehageAugust2024MedEksplisittAvslag = nåværende?.årsak != Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024 && nåværende?.erEksplisittAvslagPåSøknad == true
                (
                    nåværende?.avtaletidspunktDeltBosted != forrige?.avtaletidspunktDeltBosted ||
                        nåværende?.årsak != forrige?.årsak &&
                            erIkkeFulltidsplassIBarnehageAugust2024MedEksplisittAvslag
                )
            }

        return endringerTidslinje
    }
}
