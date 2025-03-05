package no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling

import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.avrundetHeltallAvProsent
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.inkluderer
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.medEndring
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import java.math.BigDecimal
import java.time.YearMonth

object AndelTilkjentYtelseMedEndretUtbetalingBehandler {
    fun oppdaterAndelerTilkjentYtelseMedEndretUtbetalingAndelerGammel(
        andelTilkjentYtelserUtenEndringer: List<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer.map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it) }
        }

        val nyeAndelTilkjentYtelse = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        andelTilkjentYtelserUtenEndringer.groupBy { it.aktør }.forEach { andelerForPerson ->
            val aktør = andelerForPerson.key
            val endringerForPerson = endretUtbetalingAndeler.filter { it.person?.aktør == aktør }

            val nyeAndelerForPerson = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

            andelerForPerson.value.forEach { andelForPerson ->

                // Deler opp hver enkelt andel i perioder som hhv blir berørt av endringene og de som ikke berøres av de.
                val (perioderMedEndring, perioderUtenEndring) =
                    andelForPerson
                        .stønadsPeriode()
                        .perioderMedOgUtenOverlapp(
                            endringerForPerson.map { endringerForPerson -> endringerForPerson.periode },
                        )

                // Legger til nye AndelTilkjentYtelse for perioder som er berørt av endringer.
                nyeAndelerForPerson.addAll(
                    perioderMedEndring.map { månedPeriodeEndret ->
                        val endretUtbetalingMedAndeler = endringerForPerson.single { it.overlapperMed(månedPeriodeEndret) }
                        val endretUtbetalingErAlleredeUtbetaltSomFortsattSkalUtbetales =
                            endretUtbetalingMedAndeler.årsak == Årsak.ALLEREDE_UTBETALT && endretUtbetalingMedAndeler.prosent!! > BigDecimal.ZERO

                        val nyttNasjonaltPeriodebeløp =
                            if (endretUtbetalingErAlleredeUtbetaltSomFortsattSkalUtbetales) {
                                andelForPerson.kalkulertUtbetalingsbeløp
                            } else {
                                andelForPerson.sats.avrundetHeltallAvProsent(endretUtbetalingMedAndeler.prosent!!)
                            }

                        val andelProsent =
                            if (endretUtbetalingErAlleredeUtbetaltSomFortsattSkalUtbetales) {
                                andelForPerson.prosent
                            } else {
                                endretUtbetalingMedAndeler.prosent!!
                            }

                        val andelTilkjentYtelse =
                            andelForPerson.copy(
                                prosent = andelProsent,
                                stønadFom = månedPeriodeEndret.fom,
                                stønadTom = månedPeriodeEndret.tom,
                                kalkulertUtbetalingsbeløp = nyttNasjonaltPeriodebeløp,
                                nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
                            )

                        andelTilkjentYtelse.medEndring(endretUtbetalingMedAndeler)
                    },
                )
                // Legger til nye AndelTilkjentYtelse for perioder som ikke berøres av endringer.
                nyeAndelerForPerson.addAll(
                    perioderUtenEndring.map { månedPeriodeUendret ->
                        val andelTilkjentYtelse =
                            andelForPerson.copy(
                                stønadFom = månedPeriodeUendret.fom,
                                stønadTom = månedPeriodeUendret.tom,
                            )
                        AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelse)
                    },
                )
            }

            val nyeAndelerForPersonEtterSammenslåing =
                slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
                    andelerTilkjentYtelseMedEndreteUtbetalinger = nyeAndelerForPerson,
                    skalAndelerSlåsSammen = ::skalAndelerSlåsSammen,
                )

            nyeAndelTilkjentYtelse.addAll(nyeAndelerForPersonEtterSammenslåing)
        }
        return nyeAndelTilkjentYtelse
    }

    private fun slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
        andelerTilkjentYtelseMedEndreteUtbetalinger: MutableList<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        skalAndelerSlåsSammen: (
            førsteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
            nesteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
        ) -> Boolean,
    ): MutableList<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val sorterteAndeler = andelerTilkjentYtelseMedEndreteUtbetalinger.sortedBy { it.stønadFom }.toMutableList()
        var periodenViSerPå = sorterteAndeler.first()
        val oppdatertListeMedAndeler = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        for (index in 0 until sorterteAndeler.size) {
            val andel = sorterteAndeler[index]
            val nesteAndel = if (index == sorterteAndeler.size - 1) null else sorterteAndeler[index + 1]

            periodenViSerPå =
                if (nesteAndel != null) {
                    val andelerSkalSlåsSammen =
                        skalAndelerSlåsSammen(andel, nesteAndel)

                    if (andelerSkalSlåsSammen) {
                        val nyAndel = periodenViSerPå.medTom(nesteAndel.stønadTom)
                        nyAndel
                    } else {
                        oppdatertListeMedAndeler.add(periodenViSerPå)
                        sorterteAndeler[index + 1]
                    }
                } else {
                    oppdatertListeMedAndeler.add(periodenViSerPå)
                    break
                }
        }
        return oppdatertListeMedAndeler
    }

    private fun MånedPeriode.perioderMedOgUtenOverlapp(perioder: List<MånedPeriode>): Pair<List<MånedPeriode>, List<MånedPeriode>> {
        if (perioder.isEmpty()) return Pair(emptyList(), listOf(this))

        val alleMånederMedOverlappStatus = mutableMapOf<YearMonth, Boolean>()
        var nesteMåned = this.fom
        while (nesteMåned <= this.tom) {
            alleMånederMedOverlappStatus[nesteMåned] =
                perioder.any { månedPeriode -> månedPeriode.inkluderer(nesteMåned) }
            nesteMåned = nesteMåned.plusMonths(1)
        }

        var periodeStart: YearMonth? = this.fom

        val perioderMedOverlapp = mutableListOf<MånedPeriode>()
        val perioderUtenOverlapp = mutableListOf<MånedPeriode>()
        while (periodeStart != null) {
            val periodeMedOverlapp = alleMånederMedOverlappStatus[periodeStart]!!

            val nesteMånedMedNyOverlappstatus =
                alleMånederMedOverlappStatus
                    .filter { it.key > periodeStart && it.value != periodeMedOverlapp }
                    .minByOrNull { it.key }
                    ?.key
                    ?.minusMonths(1) ?: this.tom

            // Når tom skal utledes for en periode det eksisterer en endret periode for må den minste av følgende to datoer velges:
            // 1. tom for den aktuelle endrete perioden
            // 2. neste måned uten overlappende endret periode, eller hvis null, tom for this (som representerer en AndelTilkjentYtelse).
            // Dersom tom gjelder periode uberørt av endringer så vil alltid alt.2 være korrekt.
            val periodeSlutt =
                if (periodeMedOverlapp) {
                    val nesteMånedUtenOverlapp = perioder.single { it.inkluderer(periodeStart!!) }.tom
                    minOf(nesteMånedUtenOverlapp, nesteMånedMedNyOverlappstatus)
                } else {
                    nesteMånedMedNyOverlappstatus
                }

            if (periodeMedOverlapp) {
                perioderMedOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
            } else {
                perioderUtenOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
            }

            periodeStart =
                alleMånederMedOverlappStatus
                    .filter { it.key > periodeSlutt }
                    .minByOrNull { it.key }
                    ?.key
        }
        return Pair(perioderMedOverlapp, perioderUtenOverlapp)
    }

    /**
     * Slår sammen andeler for barn når beløpet er nedjuster til 0kr som er blitt splittet av
     * for eksempel satsendring.
     */
    private fun skalAndelerSlåsSammen(
        førsteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
        nesteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
    ): Boolean =
        førsteAndel.stønadTom
            .sisteDagIInneværendeMåned()
            .erDagenFør(nesteAndel.stønadFom.førsteDagIInneværendeMåned()) &&
            førsteAndel.prosent == BigDecimal(0) &&
            nesteAndel.prosent ==
            BigDecimal(
                0,
            ) &&
            førsteAndel.endreteUtbetalinger.isNotEmpty() &&
            førsteAndel.endreteUtbetalinger.singleOrNull() == nesteAndel.endreteUtbetalinger.singleOrNull()
}
