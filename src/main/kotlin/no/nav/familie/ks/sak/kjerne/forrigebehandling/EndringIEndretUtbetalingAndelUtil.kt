package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.tilPeriode
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel

object EndringIEndretUtbetalingAndelUtil {
    fun lagEndringIEndretUbetalingAndelPerPersonTidslinje(
        nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
        forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeEndretAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()
        val forrigeTidslinje = forrigeEndretAndelerForPerson.map { it.tilPeriode() }.tilTidslinje()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                (
                    nåværende?.avtaletidspunktDeltBosted != forrige?.avtaletidspunktDeltBosted ||
                        nåværende?.årsak != forrige?.årsak ||
                        nåværende?.søknadstidspunkt != forrige?.søknadstidspunkt
                )
            }

        return endringerTidslinje
    }
}
