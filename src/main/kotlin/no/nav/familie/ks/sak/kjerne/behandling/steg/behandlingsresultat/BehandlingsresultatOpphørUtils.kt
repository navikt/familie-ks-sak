package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.tilPeriode
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

internal enum class Opphørsresultat {
    OPPHØRT,
    FORTSATT_OPPHØRT,
    IKKE_OPPHØRT,
}

object BehandlingsresultatOpphørUtils {
    internal fun hentOpphørsresultatPåBehandling(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
        nåværendePersonResultaterPåBarn: List<PersonResultat>,
        forrigePersonResultaterPåBarn: List<PersonResultat>,
        nåMåned: YearMonth,
    ): Opphørsresultat {
        val meldtOmBarnehagePlassPåAlleBarnMedLøpendeAndeler = nåværendePersonResultaterPåBarn.harMeldtOmBarnehagePlassPåAlleBarnMedLøpendeAndeler(nåværendeAndeler, nåMåned)
        val meldtOmBarnehagePlassPåAlleBarnMedLøpendeAndelerIForrigeVilkårsvurdering = forrigePersonResultaterPåBarn.harMeldtOmBarnehagePlassPåAlleBarnMedLøpendeAndeler(forrigeAndeler, nåMåned)

        if (!meldtOmBarnehagePlassPåAlleBarnMedLøpendeAndelerIForrigeVilkårsvurdering && meldtOmBarnehagePlassPåAlleBarnMedLøpendeAndeler) {
            return Opphørsresultat.OPPHØRT
        } else if (meldtOmBarnehagePlassPåAlleBarnMedLøpendeAndelerIForrigeVilkårsvurdering && meldtOmBarnehagePlassPåAlleBarnMedLøpendeAndeler) {
            return Opphørsresultat.FORTSATT_OPPHØRT
        }

        val nåværendeBehandlingOpphørsdato =
            nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
                forrigeAndelerIBehandling = forrigeAndeler,
                nåværendeEndretAndelerIBehandling = nåværendeEndretAndeler,
                endretAndelerForForrigeBehandling = forrigeEndretAndeler,
            )

        val cutOffDato = nåMåned.plusMonths(2)

        val forrigeBehandlingOpphørsdato =
            forrigeAndeler.utledOpphørsdatoForForrigeBehandling(
                forrigeEndretAndeler = forrigeEndretAndeler,
            )

        val harTidligereOpphørsDatoEnnForrigeBehandling = forrigeBehandlingOpphørsdato?.let { it > nåværendeBehandlingOpphørsdato } ?: true

        return when {
            // Rekkefølgen av sjekkene er viktig for å komme fram til riktig opphørsresultat.
            nåværendeBehandlingOpphørsdato == null && forrigeBehandlingOpphørsdato == null -> Opphørsresultat.FORTSATT_OPPHØRT
            nåværendeBehandlingOpphørsdato == null -> Opphørsresultat.IKKE_OPPHØRT // Både forrige og nåværende behandling har ingen andeler
            nåværendeBehandlingOpphørsdato <= cutOffDato && harTidligereOpphørsDatoEnnForrigeBehandling -> Opphørsresultat.OPPHØRT // Nåværende behandling er opphørt og forrige har senere opphørsdato
            nåværendeBehandlingOpphørsdato <= cutOffDato && nåværendeBehandlingOpphørsdato == forrigeBehandlingOpphørsdato -> Opphørsresultat.FORTSATT_OPPHØRT
            else -> Opphørsresultat.IKKE_OPPHØRT
        }
    }

    private fun List<PersonResultat>.harMeldtOmBarnehagePlassPåAlleBarnMedLøpendeAndeler(
        andelerIBehandling: List<AndelTilkjentYtelse>,
        nåMåned: YearMonth,
    ): Boolean {
        val personResultaterForBarnMedLøpendeAndeler = this.filter { barn -> andelerIBehandling.any { it.aktør == barn.aktør && it.erLøpende(nåMåned) } }

        val meldtBarnehageplassPåAlleBarnMedLøpendeAndeler =
            personResultaterForBarnMedLøpendeAndeler.isNotEmpty() &&
                personResultaterForBarnMedLøpendeAndeler.all { personResultatForBarn ->
                    personResultatForBarn.vilkårResultater.any { vilkårResultat ->
                        vilkårResultat.harMeldtBarnehageplassOgErFulltidIBarnehage()
                    }
                }

        return meldtBarnehageplassPåAlleBarnMedLøpendeAndeler
    }

    private fun List<AndelTilkjentYtelse>.finnOpphørsdato() = this.maxOfOrNull { it.stønadTom }?.nesteMåned()

    /**
     * Hvis opphørsdato ikke finnes i denne behandlingen så ønsker vi å bruke tidligste fom-dato fra forrige behandling
     * Ingen opphørsdato i denne behandlingen skjer kun hvis det ikke finnes noen andeler, og da har vi to scenarier:
     * 1. Ingen andeler i denne behandlingen, men andeler i forrige behandling. Da ønsker vi at opphørsdatoen i denne behandlingen skal være "første endring" som altså er lik tidligste fom-dato
     * 2. Ingen andeler i denne behandlingen, ingen andeler i forrige behandling. Da vil denne funksjonen returnere null
     */
    fun List<AndelTilkjentYtelse>.utledOpphørsdatoForNåværendeBehandlingMedFallback(
        forrigeAndelerIBehandling: List<AndelTilkjentYtelse>,
        nåværendeEndretAndelerIBehandling: List<EndretUtbetalingAndel>,
        endretAndelerForForrigeBehandling: List<EndretUtbetalingAndel>,
    ): YearMonth? = this.filtrerBortIrrelevanteAndeler(endretAndeler = nåværendeEndretAndelerIBehandling).finnOpphørsdato() ?: forrigeAndelerIBehandling.filtrerBortIrrelevanteAndeler(endretAndeler = endretAndelerForForrigeBehandling).minOfOrNull { it.stønadFom }

    /**
     * Hvis det ikke fantes noen andeler i forrige behandling defaulter vi til inneværende måned
     */
    private fun List<AndelTilkjentYtelse>.utledOpphørsdatoForForrigeBehandling(forrigeEndretAndeler: List<EndretUtbetalingAndel>): YearMonth? = this.filtrerBortIrrelevanteAndeler(endretAndeler = forrigeEndretAndeler).finnOpphørsdato()

    /**
     * Hvis det eksisterer andeler med beløp == 0 så ønsker vi å filtrere bort disse dersom det eksisterer endret utbetaling andel for perioden
     * med årsak ALLEREDE_UTBETALT, ENDRE_MOTTAKER eller ETTERBETALING_3ÅR. Vi grupperer type andeler før vi oppretter tidslinjer da det kan oppstå
     * overlapp hvis vi ikke gjør dette.
     */
    internal fun List<AndelTilkjentYtelse>.filtrerBortIrrelevanteAndeler(endretAndeler: List<EndretUtbetalingAndel>): List<AndelTilkjentYtelse> {
        val personerMedAndeler = this.map { it.aktør }.distinct()

        return personerMedAndeler.flatMap { aktør ->
            val andelerGruppertPerTypePåPerson = this.filter { it.aktør == aktør }.groupBy { it.type }
            val endretUtbetalingAndelerPåPerson = endretAndeler.filter { it.personer.any { person -> person.aktør == aktør } }

            andelerGruppertPerTypePåPerson.values.flatMap { andelerPerType ->
                filtrerBortIrrelevanteAndelerPerPersonOgType(andelerPerType, endretUtbetalingAndelerPåPerson)
            }
        }
    }

    private fun filtrerBortIrrelevanteAndelerPerPersonOgType(
        andelerPåPersonFiltrertPåType: List<AndelTilkjentYtelse>,
        endretAndelerPåPerson: List<EndretUtbetalingAndel>,
    ): List<AndelTilkjentYtelse> {
        val andelTilkjentYtelseTidslinje = andelerPåPersonFiltrertPåType.map { it.tilPeriode() }.tilTidslinje()
        val endretUtbetalingAndelTidslinje = endretAndelerPåPerson.map { it.tilPeriode() }.tilTidslinje()

        return andelTilkjentYtelseTidslinje
            .kombinerMed(endretUtbetalingAndelTidslinje) { andelTilkjentYtelse, endretUtbetalingAndel ->
                val kalkulertUtbetalingsbeløp = andelTilkjentYtelse?.kalkulertUtbetalingsbeløp ?: return@kombinerMed null
                val endringsperiodeÅrsak = endretUtbetalingAndel?.årsak ?: return@kombinerMed andelTilkjentYtelse

                when (endringsperiodeÅrsak) {
                    Årsak.ALLEREDE_UTBETALT,
                    Årsak.ETTERBETALING_3MND,
                    Årsak.FULLTIDSPLASS_I_BARNEHAGE_AUGUST_2024,
                    ->
                        // Vi ønsker å filtrere bort andeler som har 0 i kalkulertUtbetalingsbeløp
                        if (kalkulertUtbetalingsbeløp == 0) null else andelTilkjentYtelse
                }
            }.tilAndelerTilkjentYtelse()
    }
}
