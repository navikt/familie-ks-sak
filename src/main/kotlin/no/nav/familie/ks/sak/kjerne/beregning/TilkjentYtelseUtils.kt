package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.avrundetHeltallAvProsent
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.inkluderer
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.SatsPeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {
    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        erToggleForLovendringAugust2024På: Boolean,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
    ): TilkjentYtelse {
        val tilkjentYtelse =
            TilkjentYtelse(
                behandling = vilkårsvurdering.behandling,
                opprettetDato = LocalDate.now(),
                endretDato = LocalDate.now(),
            )
        val endretUtbetalingAndelerBarna = endretUtbetalingAndeler.filter { it.person?.type == PersonType.BARN }

        val andelerTilkjentYtelseBarnaUtenEndringer =
            beregnAndelerTilkjentYtelseForBarna(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
                erToggleForLovendringAugust2024På = erToggleForLovendringAugust2024På,
            )

        val andelerTilkjentYtelseBarnaMedAlleEndringer =
            oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
                andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
                endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
            )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel })
        return tilkjentYtelse
    }

    fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
        erToggleForLovendringAugust2024På: Boolean,
    ): List<AndelTilkjentYtelse> {
        val søkersVilkårResultaterForskjøvetTidslinje =
            vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
                personopplysningGrunnlag.søker,
                erToggleForLovendringAugust2024På,
            )

        return personopplysningGrunnlag.barna.flatMap { barn ->
            val barnetsVilkårResultaterForskjøvetTidslinje =
                vilkårsvurdering.personResultater.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(barn, erToggleForLovendringAugust2024På)

            val barnVilkårResultaterForskjøvetBådeBarnOgSøkerHarAlleOppfylt =
                barnetsVilkårResultaterForskjøvetTidslinje.kombinerMed(
                    søkersVilkårResultaterForskjøvetTidslinje,
                ) { barnPeriode, søkerPeriode ->
                    søkerPeriode?.let { barnPeriode }
                }

            barnVilkårResultaterForskjøvetBådeBarnOgSøkerHarAlleOppfylt
                .tilPerioderIkkeNull()
                .map { vilkårResultaterPeriode ->
                    vilkårResultaterPeriode.tilAndelTilkjentYtelse(
                        vilkårsvurdering = vilkårsvurdering,
                        tilkjentYtelse = tilkjentYtelse,
                        barn = barn,
                    )
                }
        }
    }

    private fun Periode<List<VilkårResultat>>.tilAndelTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse,
        barn: Person,
    ): AndelTilkjentYtelse {
        val erDeltBosted =
            this.verdi.any {
                it.vilkårType == Vilkår.BOR_MED_SØKER &&
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
            }

        val antallTimer = this.verdi.single { it.vilkårType == Vilkår.BARNEHAGEPLASS }.antallTimer

        val satsperiode =
            hentGyldigSatsFor(
                antallTimer = antallTimer?.setScale(2, RoundingMode.HALF_UP),
                erDeltBosted = erDeltBosted,
                stønadFom = fom!!.toYearMonth(),
                stønadTom = tom!!.toYearMonth(),
            )

        validerBeregnetPeriode(beløpsperiode = satsperiode, behandlingId = vilkårsvurdering.behandling.id)

        val kalkulertUtbetalingsbeløp = satsperiode.sats.prosent(satsperiode.prosent)

        return AndelTilkjentYtelse(
            behandlingId = vilkårsvurdering.behandling.id,
            tilkjentYtelse = tilkjentYtelse,
            aktør = barn.aktør,
            stønadFom = satsperiode.fom,
            stønadTom = satsperiode.tom,
            kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
            nasjonaltPeriodebeløp = kalkulertUtbetalingsbeløp,
            type = YtelseType.ORDINÆR_KONTANTSTØTTE,
            sats = satsperiode.sats,
            prosent = satsperiode.prosent,
        )
    }

    private fun validerBeregnetPeriode(
        beløpsperiode: SatsPeriode,
        behandlingId: Long,
    ) {
        if (beløpsperiode.fom.isAfter(beløpsperiode.tom)) {
            throw Feil(
                "Feil i beregning for behandling $behandlingId," +
                    "fom ${beløpsperiode.fom} kan ikke være større enn tom ${beløpsperiode.tom}",
            )
        }
    }

    fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
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
                        val endretUtbetalingMedAndeler =
                            endringerForPerson.single { it.overlapperMed(månedPeriodeEndret) }
                        val nyttNasjonaltPeriodebeløp =
                            andelForPerson.sats
                                .avrundetHeltallAvProsent(endretUtbetalingMedAndeler.prosent!!)

                        val andelTilkjentYtelse =
                            andelForPerson.copy(
                                prosent = endretUtbetalingMedAndeler.prosent!!,
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

    fun MånedPeriode.perioderMedOgUtenOverlapp(perioder: List<MånedPeriode>): Pair<List<MånedPeriode>, List<MånedPeriode>> {
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

    fun slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
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
