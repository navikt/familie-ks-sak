package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.forrigeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toLocalDate
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriode
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringIEndretUtbetalingAndelUtil
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringIKompetanseUtil
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringIVilkårsvurderingUtil
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.klipp
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.YearMonth

internal enum class Endringsresultat {
    ENDRING,
    INGEN_ENDRING,
}

object BehandlingsresultatEndringUtils {
    internal fun utledEndringsresultat(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
        nåværendePersonResultater: Set<PersonResultat>,
        forrigePersonResultater: Set<PersonResultat>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
        personerIForrigeBehandling: Set<Person>,
    ): Endringsresultat {
        val logger: Logger = LoggerFactory.getLogger("utledEndringsresultatLogger")
        val secureLogger = LoggerFactory.getLogger("secureLogger")

        val relevantePersoner = personerIForrigeBehandling.map { it.aktør }

        val endringerForRelevantePersoner =
            relevantePersoner.any { aktør ->
                val nåværendePersonResultat = nåværendePersonResultater.singleOrNull { it.aktør == aktør }
                val forrigePersonResultat = forrigePersonResultater.singleOrNull { it.aktør == aktør }

                val nåværendeAndelerForPerson = nåværendeAndeler.filter { it.aktør == aktør }
                val forrigeAndelerForPerson = forrigeAndeler.filter { it.aktør == aktør }

                val nåværendeEndretAndelerForPerson = nåværendeEndretAndeler.filter { it.person?.aktør == aktør }
                val forrigeEndretAndelerForPerson = forrigeEndretAndeler.filter { it.person?.aktør == aktør }

                val nåværendeKompetanserForPerson = nåværendeKompetanser.filter { it.barnAktører.contains(aktør) }
                val forrigeKompetanserForPerson = forrigeKompetanser.filter { it.barnAktører.contains(aktør) }

                val opphørstidspunktForBehandling =
                    nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                        forrigeAndelerIBehandling = forrigeAndeler,
                        nåværendeEndretAndelerIBehandling = nåværendeEndretAndeler,
                        endretAndelerForForrigeBehandling = forrigeEndretAndeler,
                    )

                val erEndringIBeløpForPerson =
                    opphørstidspunktForBehandling?.let {
                        erEndringIBeløpForPerson(
                            nåværendeAndelerForPerson = nåværendeAndelerForPerson,
                            forrigeAndelerForPerson = forrigeAndelerForPerson,
                            opphørstidspunktForBehandling = opphørstidspunktForBehandling,
                            erFremstiltKravForPerson = personerFremstiltKravFor.contains(aktør),
                        )
                    } ?: false // false hvis verken forrige eller nåværende behandling har andeler

                val erEndringIVilkårsvurderingForPerson =
                    erEndringIVilkårsvurderingForPerson(
                        nåværendePersonResultat = nåværendePersonResultat,
                        forrigePersonResultat = forrigePersonResultat,
                    )

                val erEndringIKompetanseForPerson =
                    erEndringIKompetanseForPerson(
                        nåværendeKompetanserForPerson = nåværendeKompetanserForPerson,
                        forrigeKompetanserForPerson = forrigeKompetanserForPerson,
                    )

                val erEndringIEndretUtbetalingAndelerForPerson =
                    erEndringIEndretUtbetalingAndelerForPerson(
                        nåværendeEndretAndelerForPerson = nåværendeEndretAndelerForPerson,
                        forrigeEndretAndelerForPerson = forrigeEndretAndelerForPerson,
                    )

                val erMinstEnEndringForPerson =
                    erEndringIBeløpForPerson ||
                        erEndringIKompetanseForPerson ||
                        erEndringIVilkårsvurderingForPerson ||
                        erEndringIEndretUtbetalingAndelerForPerson

                if (erMinstEnEndringForPerson) {
                    logger.info(
                        "Endringer: " +
                            "erEndringIBeløp=$erEndringIBeløpForPerson for aktør ${aktør.aktørId}," +
                            "erEndringIKompetanse=$erEndringIKompetanseForPerson for aktør ${aktør.aktørId}, " +
                            "erEndringIVilkårsvurdering=$erEndringIVilkårsvurderingForPerson for aktør ${aktør.aktørId}, " +
                            "erEndringIEndretUtbetalingAndeler=$erEndringIEndretUtbetalingAndelerForPerson for aktør ${aktør.aktørId}",
                    )

                    val endredeAndelTilkjentYtelseForPerson = if (erEndringIBeløpForPerson) "nye AndelerTilkjentYtelse for aktør ${aktør.aktørId}: $nåværendeAndeler , " else ""
                    val endredeKompetanserForPerson = if (erEndringIKompetanseForPerson) "nye kompetanser for aktør ${aktør.aktørId}: $nåværendeKompetanser ," else ""
                    val endredeVilkårsvurderingerForPerson = if (erEndringIVilkårsvurderingForPerson) "nye personresultater for aktør ${aktør.aktørId}: $nåværendePersonResultater ," else ""
                    val endredeEndretUtbetalingAndelerForPerson = if (erEndringIEndretUtbetalingAndelerForPerson) "nye endretUtbetalingAndeler for aktør ${aktør.aktørId}: $nåværendeEndretAndeler" else ""

                    secureLogger.info(
                        "Endringer: $endredeAndelTilkjentYtelseForPerson $endredeKompetanserForPerson $endredeVilkårsvurderingerForPerson $endredeEndretUtbetalingAndelerForPerson",
                    )
                }

                erMinstEnEndringForPerson
            }

        return if (endringerForRelevantePersoner) Endringsresultat.ENDRING else Endringsresultat.INGEN_ENDRING
    }

    // NB: For personer fremstilt krav for tar vi ikke hensyn til alle endringer i beløp i denne funksjonen
    internal fun erEndringIBeløpForPerson(
        nåværendeAndelerForPerson: List<AndelTilkjentYtelse>,
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>,
        erFremstiltKravForPerson: Boolean,
        opphørstidspunktForBehandling: YearMonth,
    ): Boolean {
        val ytelseTyperForPerson = (nåværendeAndelerForPerson.map { it.type } + forrigeAndelerForPerson.map { it.type }).map { it.tilYtelseType() }

        return ytelseTyperForPerson.any { ytelseType ->
            erEndringIBeløpForPersonOgType(
                nåværendeAndeler = nåværendeAndelerForPerson.filter { it.type.tilYtelseType() == ytelseType },
                forrigeAndeler = forrigeAndelerForPerson.filter { it.type.tilYtelseType() == ytelseType },
                opphørstidspunktForBehandling = opphørstidspunktForBehandling,
                erFremstiltKravForPerson = erFremstiltKravForPerson,
            )
        }
    }
}

// Kun interessert i endringer i beløp FØR opphørstidspunkt og perioder som ikke er lengre enn 1 måned fram i tid
private fun erEndringIBeløpForPersonOgType(
    nåværendeAndeler: List<AndelTilkjentYtelse>,
    forrigeAndeler: List<AndelTilkjentYtelse>,
    opphørstidspunktForBehandling: YearMonth,
    erFremstiltKravForPerson: Boolean,
): Boolean {
    val nåværendeTidslinje = nåværendeAndeler.map { it.tilPeriode() }.tilTidslinje()
    val forrigeTidslinje = forrigeAndeler.map { it.tilPeriode() }.tilTidslinje()

    val endringIBeløpTidslinje =
        nåværendeTidslinje
            .kombinerMed(forrigeTidslinje) { nåværende, forrige ->
                val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
                val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

                if (erFremstiltKravForPerson) {
                    // Hvis det er søkt for person vil vi kun ha med endringer som går fra beløp > 0 til 0/null siden vi skal ha innvilgelse når vi øker beløpet.
                    forrigeBeløp > 0 && nåværendeBeløp == 0
                } else {
                    // Hvis det ikke er søkt for person vil vi ha med alle endringer i beløp
                    forrigeBeløp != nåværendeBeløp
                }
            }.fjernPerioderEtterOpphørsdato(opphørstidspunktForBehandling)

    return endringIBeløpTidslinje.tilPerioder().any { it.verdi == true }
}

private fun Tidslinje<Boolean>.fjernPerioderEtterOpphørsdato(opphørstidspunkt: YearMonth) = this.klipp(startTidspunkt = TIDENES_MORGEN, sluttTidspunkt = opphørstidspunkt.forrigeMåned().toLocalDate().sisteDagIMåned())

internal fun erEndringIKompetanseForPerson(
    nåværendeKompetanserForPerson: List<Kompetanse>,
    forrigeKompetanserForPerson: List<Kompetanse>,
): Boolean {
    val endringIKompetanseTidslinje =
        EndringIKompetanseUtil.lagEndringIKompetanseForPersonTidslinje(
            nåværendeKompetanserForPerson = nåværendeKompetanserForPerson,
            forrigeKompetanserForPerson = forrigeKompetanserForPerson,
        )

    return endringIKompetanseTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIVilkårsvurderingForPerson(
    nåværendePersonResultat: PersonResultat?,
    forrigePersonResultat: PersonResultat?,
): Boolean {
    val endringIVilkårsvurderingTidslinje =
        EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultat = nåværendePersonResultat,
            forrigePersonResultat = forrigePersonResultat,
        )

    return endringIVilkårsvurderingTidslinje.tilPerioder().any { it.verdi == true }
}

internal fun erEndringIEndretUtbetalingAndelerForPerson(
    nåværendeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
    forrigeEndretAndelerForPerson: List<EndretUtbetalingAndel>,
): Boolean {
    val endringIEndretUtbetalingAndelTidslinje =
        EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUbetalingAndelPerPersonTidslinje(
            nåværendeEndretAndelerForPerson = nåværendeEndretAndelerForPerson,
            forrigeEndretAndelerForPerson = forrigeEndretAndelerForPerson,
        )

    return endringIEndretUtbetalingAndelTidslinje.tilPerioder().any { it.verdi == true }
}
