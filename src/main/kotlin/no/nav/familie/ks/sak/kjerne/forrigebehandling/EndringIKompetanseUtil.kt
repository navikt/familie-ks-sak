package no.nav.familie.ks.sak.kjerne.forrigebehandling

import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilTidslinje
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

object EndringIKompetanseUtil {
    fun utledEndringstidspunktForKompetanse(
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
    ): YearMonth? {
        val forrigeAktører = forrigeKompetanser.flatMap { it.barnAktører }
        val nåværendeAktører = nåværendeKompetanser.flatMap { it.barnAktører }
        val alleAktørerMedKompetanse = (nåværendeAktører + forrigeAktører).distinct()

        val endringIKompetanseTidslinjer =
            alleAktørerMedKompetanse
                .map { aktør ->
                    lagEndringIKompetanseForPersonTidslinje(
                        nåværendeKompetanserForPerson = nåværendeKompetanser.filter { it.barnAktører.contains(aktør) },
                        forrigeKompetanserForPerson = forrigeKompetanser.filter { it.barnAktører.contains(aktør) },
                    )
                }.kombiner { finnesMinstEnEndringIPeriode(it) }

        return endringIKompetanseTidslinjer.tilFørsteEndringstidspunkt()
    }

    fun lagEndringIKompetanseForPersonTidslinje(
        nåværendeKompetanserForPerson: List<Kompetanse>,
        forrigeKompetanserForPerson: List<Kompetanse>,
    ): Tidslinje<Boolean> {
        val nåværendeTidslinje = nåværendeKompetanserForPerson.tilTidslinje().filtrerIkkeNull()
        val forrigeTidslinje = forrigeKompetanserForPerson.tilTidslinje().filtrerIkkeNull()

        val endringerTidslinje =
            nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                if (nåværende == null || forrige == null) return@kombinerMed false

                forrige.erObligatoriskeFelterUtenomTidsperioderSatt() && nåværende.felterHarEndretSegSidenForrigeBehandling(forrigeKompetanse = forrige)
            }

        return endringerTidslinje
    }

    private fun Kompetanse.felterHarEndretSegSidenForrigeBehandling(forrigeKompetanse: Kompetanse): Boolean =
        this.søkersAktivitet != forrigeKompetanse.søkersAktivitet ||
            this.søkersAktivitetsland != forrigeKompetanse.søkersAktivitetsland ||
            this.annenForeldersAktivitet != forrigeKompetanse.annenForeldersAktivitet ||
            this.annenForeldersAktivitetsland != forrigeKompetanse.annenForeldersAktivitetsland ||
            this.barnetsBostedsland != forrigeKompetanse.barnetsBostedsland ||
            this.resultat != forrigeKompetanse.resultat

    private fun finnesMinstEnEndringIPeriode(
        endringer: Iterable<Boolean>,
    ): Boolean = endringer.any { it }
}
