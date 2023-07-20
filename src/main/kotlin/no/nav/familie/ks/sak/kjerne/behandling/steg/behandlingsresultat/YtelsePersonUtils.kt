package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erSammeEllerTidligere
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.inneværendeMåned
import no.nav.familie.ks.sak.common.util.nesteMåned
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.AVSLÅTT
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.ENDRET_UTBETALING
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.ENDRET_UTEN_UTBETALING
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.FORTSATT_OPPHØRT
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.IKKE_VURDERT
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat.YtelsePersonResultat.OPPHØRT
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.apache.commons.lang3.RandomStringUtils
import java.time.YearMonth

object YtelsePersonUtils {

    /**
     * Utleder hvilke konsekvenser _denne_ behandlingen har for personen og populerer "resultater" med utfallet.
     *
     * @param [behandlingsresultatPersoner] Personer som er vurdert i behandlingen med metadata
     * @param [uregistrerteBarn] Barn det er søkt for som ikke er folkeregistrert
     * @return Personer populert med utfall (resultater) etter denne behandlingen
     */
    fun utledYtelsePersonerMedResultat(
        behandlingsresultatPersoner: List<BehandlingsresultatPerson>,
        uregistrerteBarn: List<String> = emptyList(),
    ): List<YtelsePerson> {
        val altOpphørt = behandlingsresultatPersoner.all { erYtelsenOpphørt(it.andeler) }

        return behandlingsresultatPersoner.map { behandlingsresultatPerson ->
            val forrigeAndeler = behandlingsresultatPerson.forrigeAndeler
            val andeler = behandlingsresultatPerson.andeler

            val tidslinjeForForrigeAndeler = forrigeAndeler.tilTidslinje()
            val tidslinjeForAndeler = andeler.tilTidslinje()

            val erBeløpEndretTidslinje =
                tidslinjeForAndeler.kombinerMed(tidslinjeForForrigeAndeler) { andel, andelForrigeBehandling ->
                    andel != null && andelForrigeBehandling != null && andel.kalkulertUtbetalingsbeløp != andelForrigeBehandling.kalkulertUtbetalingsbeløp
                }.tilPerioder().filtrerIkkeNull()

            val perioderLagtTil = tidslinjeForAndeler.kombinerMed(tidslinjeForForrigeAndeler) { verdi1, verdi2 ->
                if (verdi2 == null) verdi1 else null
            }.tilPerioder().filtrerIkkeNull()

            val perioderFjernet = tidslinjeForForrigeAndeler.kombinerMed(tidslinjeForAndeler) { verdi1, verdi2 ->
                if (verdi2 == null) verdi1 else null
            }.tilPerioder().filtrerIkkeNull()

            val harSammeTidslinje = andeler.isNotEmpty() && forrigeAndeler.isNotEmpty() &&
                tidslinjeForForrigeAndeler == tidslinjeForAndeler

            val resultater = mutableSetOf<YtelsePersonResultat>()
            val ytelsePerson = behandlingsresultatPerson.utledYtelsePerson()

            // 1. sjekk avslag
            if (behandlingsresultatPerson.eksplisittAvslag || avslagPåNyPerson(ytelsePerson, perioderLagtTil)) {
                resultater.add(AVSLÅTT)
            }

            // 2. sjekk opphørt
            if (erYtelsenOpphørt(andeler = andeler)) {
                when {
                    // ytelsen er opphørt ved dødsfall. Da kan RV har samme tidstilnje men alle ytelesene er opphørt
                    harSammeTidslinje && altOpphørt -> resultater.add(FORTSATT_OPPHØRT)
                    (perioderFjernet + perioderLagtTil).isNotEmpty() -> resultater.add(OPPHØRT)
                }
            }

            // 3. sjekk innvilget
            if (finnesInnvilget(behandlingsresultatPerson, perioderLagtTil)) resultater.add(INNVILGET)

            val ytelseSlutt: YearMonth = when {
                andeler.isNotEmpty() -> andeler.maxByOrNull { it.stønadTom }?.stønadTom
                    ?: throw Feil("Finnes andel uten tom")
                else -> TIDENES_MORGEN.toYearMonth()
            }

            // 4. sjekk endring
            utledYtelsePersonResultatVedEndring(
                behandlingsresultatPerson = behandlingsresultatPerson,
                perioderLagtTil = perioderLagtTil,
                perioderFjernet = perioderFjernet,
                erBeløpEndretTidslinje = erBeløpEndretTidslinje,
            ).let { if (it != IKKE_VURDERT) resultater.add(it) }

            ytelsePerson.copy(resultater = resultater, ytelseSlutt = ytelseSlutt)
        } + uregistrerteBarn.map {
            YtelsePerson(
                aktør = Aktør(RandomStringUtils.randomNumeric(13)), // Aktør med dummy aktørId
                resultater = setOf(AVSLÅTT),
                ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ytelseSlutt = TIDENES_MORGEN.toYearMonth(),
                kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
            )
        }
    }

    fun validerYtelsePersoner(ytelsePersoner: List<YtelsePerson>) {
        if (ytelsePersoner.flatMap { it.resultater }.any { it == IKKE_VURDERT }) {
            throw Feil(message = "Minst én ytelseperson er ikke vurdert")
        }

        if (ytelsePersoner.any { it.ytelseSlutt == null }) {
            throw Feil(message = "YtelseSlutt er ikke satt ved utledning av behandlingsresultat")
        }

        if (ytelsePersoner.any {
            it.resultater.contains(OPPHØRT) && it.ytelseSlutt?.isAfter(inneværendeMåned()) == true
        }
        ) {
            throw Feil(message = "Minst én ytelseperson har fått opphør som resultat og ytelseSlutt etter inneværende måned")
        }
    }

    fun oppdaterYtelsePersonResultaterVedOpphør(ytelsePersoner: List<YtelsePerson>): Set<YtelsePersonResultat> {
        val resultater = ytelsePersoner.flatMap { it.resultater }.toMutableSet()
        val erKunFremstilKravIDenneBehandling =
            ytelsePersoner.flatMap { it.kravOpprinnelse }.all { it == KravOpprinnelse.INNEVÆRENDE }

        val kunFortsattOpphørt = resultater.all { it == FORTSATT_OPPHØRT }
        val erAvslått = resultater.all { it == AVSLÅTT }

        val altOpphører =
            ytelsePersoner.all { it.ytelseSlutt != null && it.ytelseSlutt.erSammeEllerTidligere(inneværendeMåned()) }
        val noeOpphørerPåTidligereBarn = ytelsePersoner.any {
            it.resultater.contains(OPPHØRT) && !it.kravOpprinnelse.contains(KravOpprinnelse.INNEVÆRENDE)
        }
        // alle barn har opphørt på samme dato, mao alle barn har samme ytelseSlutt og eller alle barn får avslått
        val opphørPåSammeTid = altOpphører && (
            ytelsePersoner.filter { it.resultater != setOf(AVSLÅTT) }
                .groupBy { it.ytelseSlutt }.size == 1 || erAvslått
            )

        // Hvis alt ikke er opphørt, kan ikke resultater ha Opphørt
        if (!altOpphører) resultater.remove(OPPHØRT)

        // Hvis noen opphører for tidligere barn og alt opphører ikke, betyr det er endring i utbetaling, kanskje redusert utbetaling
        if (noeOpphørerPåTidligereBarn && !altOpphører) resultater.add(ENDRET_UTBETALING)

        // opphør som fører til endring
        val opphørSomFørerTilEndring =
            altOpphører && !erKunFremstilKravIDenneBehandling && !kunFortsattOpphørt && !opphørPåSammeTid
        if (opphørSomFørerTilEndring) resultater.add(ENDRET_UTBETALING)

        return resultater
    }

    private fun erYtelsenOpphørt(andeler: List<BehandlingsresultatAndelTilkjentYtelse>) =
        andeler.none { it.erLøpende(YearMonth.now()) }

    private fun List<BehandlingsresultatAndelTilkjentYtelse>.tilTidslinje(): Tidslinje<BehandlingsresultatAndelTilkjentYtelse> =
        this.map {
            Periode(
                verdi = it,
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned(),
            )
        }.tilTidslinje()

    private fun avslagPåNyPerson(
        personSomSjekkes: YtelsePerson,
        perioderLagtTil: List<Periode<BehandlingsresultatAndelTilkjentYtelse>>,
    ) = personSomSjekkes.kravOpprinnelse == listOf(KravOpprinnelse.INNEVÆRENDE) && perioderLagtTil.isEmpty()

    private fun finnesInnvilget(
        behandlingsresultatPerson: BehandlingsresultatPerson,
        perioderLagtTil: List<Periode<BehandlingsresultatAndelTilkjentYtelse>>,
    ) = behandlingsresultatPerson.utledYtelsePerson()
        .erFramstiltKravForIInneværendeBehandling() && (
        perioderLagtTil.isNotEmpty() || andelerMedEndretBeløp(
            behandlingsresultatPerson.forrigeAndeler,
            behandlingsresultatPerson.andeler,
        ).any { it > 0 }
        )

    private fun andelerMedEndretBeløp(
        forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse>,
        andeler: List<BehandlingsresultatAndelTilkjentYtelse>,
    ): List<Int> = andeler.flatMap { andel ->
        val andelerFraForrigeBehandlingISammePeriode = forrigeAndeler.filter {
            it.periode.overlapperHeltEllerDelvisMed(andel.periode)
        }

        andelerFraForrigeBehandlingISammePeriode.map {
            andel.kalkulertUtbetalingsbeløp - it.kalkulertUtbetalingsbeløp
        }.filter { it != 0 }
    }

    private fun utledYtelsePersonResultatVedEndring(
        behandlingsresultatPerson: BehandlingsresultatPerson,
        perioderLagtTil: List<Periode<BehandlingsresultatAndelTilkjentYtelse>>,
        perioderFjernet: List<Periode<BehandlingsresultatAndelTilkjentYtelse>>,
        erBeløpEndretTidslinje: List<Periode<Boolean>>,
    ): YtelsePersonResultat {
        val inneværendeMåned = YearMonth.now()
        val nesteMåned = inneværendeMåned.nesteMåned()
        val andeler = behandlingsresultatPerson.andeler
        val forrigeAndeler = behandlingsresultatPerson.forrigeAndeler
        val ytelsePerson = behandlingsresultatPerson.utledYtelsePerson()
        val erFramstiltKravForITidligereBehandling = ytelsePerson.erFramstiltKravForITidligereBehandling()

        val stønadSlutt = andeler.maxByOrNull { it.stønadFom }?.stønadTom
            ?: TIDENES_MORGEN.toYearMonth()

        val forrigeStønadSlutt = forrigeAndeler.maxByOrNull { it.stønadFom }?.stønadTom
            ?: TIDENES_MORGEN.toYearMonth()

        val opphører = stønadSlutt.isBefore(nesteMåned)

        val erPeriodeMedEndretBeløp = erBeløpEndretTidslinje.any { it.verdi }

        return when {
            behandlingsresultatPerson.søktForPerson -> {
                val beløpRedusert = (perioderLagtTil + perioderFjernet).isEmpty() &&
                    (forrigeAndeler.sumOf { it.sumForPeriode() } - andeler.sumOf { it.sumForPeriode() }) > 0
                val finnesReduksjonerTilbakeITid = erFramstiltKravForITidligereBehandling &&
                    perioderFjernet.harPeriodeFør(inneværendeMåned)

                val finnesReduksjonerTilbakeITidMedBeløp = finnesReduksjonerTilbakeITid &&
                    perioderFjernet.any { it.verdi.kalkulertUtbetalingsbeløp > 0 }

                when {
                    opphører -> if (erPeriodeMedEndretBeløp) ENDRET_UTBETALING else IKKE_VURDERT
                    beløpRedusert || finnesReduksjonerTilbakeITidMedBeløp -> ENDRET_UTBETALING
                    finnesReduksjonerTilbakeITid -> ENDRET_UTEN_UTBETALING
                    else -> IKKE_VURDERT
                }
            }
            forrigeAndeler.isNotEmpty() -> {
                val erAndelMedEndretBeløp = andelerMedEndretBeløp(
                    forrigeAndeler = behandlingsresultatPerson.forrigeAndeler,
                    andeler = behandlingsresultatPerson.andeler,
                ).isNotEmpty()

                val erPerioderLagtTil = erFramstiltKravForITidligereBehandling &&
                    perioderLagtTil.harPeriodeFør(if (opphører) stønadSlutt else nesteMåned)

                val erLagtTilPerioderMedEndringIUtbetaling = erPerioderLagtTil &&
                    perioderLagtTil.any { it.verdi.kalkulertUtbetalingsbeløp > 0 }

                val erPerioderFjernet = perioderFjernet.harPeriodeFør(if (opphører) stønadSlutt else nesteMåned)

                val erFjernetPerioderMedEndringIUtbetaling = erPerioderFjernet &&
                    perioderFjernet.any { it.verdi.kalkulertUtbetalingsbeløp > 0 }

                val opphørsdatoErSattSenere = stønadSlutt.isAfter(forrigeStønadSlutt)

                when {
                    erAndelMedEndretBeløp ||
                        erLagtTilPerioderMedEndringIUtbetaling ||
                        erFjernetPerioderMedEndringIUtbetaling ||
                        opphørsdatoErSattSenere -> ENDRET_UTBETALING
                    erPerioderLagtTil || erPerioderFjernet -> ENDRET_UTEN_UTBETALING
                    else -> IKKE_VURDERT
                }
            }
            else -> IKKE_VURDERT
        }
    }

    private fun List<Periode<BehandlingsresultatAndelTilkjentYtelse>>.harPeriodeFør(måned: YearMonth) =
        this.isNotEmpty() && (
            this.any { it.tom != null && it.tom < måned.sisteDagIInneværendeMåned() } ||
                this.any { it.fom != null && it.fom < måned.sisteDagIInneværendeMåned() }
            )
}
