package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.erBack2BackIMånedsskifte
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.BarnehageplassEndringstype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.tilFørskjøvetVilkårResultatTidslinjeMap
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.SatsPeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.RoundingMode
import java.time.LocalDate

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList()
    ): TilkjentYtelse {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = vilkårsvurdering.behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )
        val endretUtbetalingAndelerBarna = endretUtbetalingAndeler.filter { it.person?.type == PersonType.BARN }

        val andelerTilkjentYtelseBarnaUtenEndringer = beregnAndelerTilkjentYtelseForBarna(
            personopplysningGrunnlag = personopplysningGrunnlag,
            vilkårsvurdering = vilkårsvurdering,
            tilkjentYtelse = tilkjentYtelse
        )

        val andelerTilkjentYtelseBarnaMedAlleEndringer = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
            endretUtbetalingAndeler = endretUtbetalingAndelerBarna
        )
        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel })
        return tilkjentYtelse
    }

    private fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse
    ): List<AndelTilkjentYtelse> {
        val førskjøvetVilkårResultatTidslinjeMap =
            vilkårsvurdering.personResultater.tilFørskjøvetVilkårResultatTidslinjeMap(personopplysningGrunnlag)

        val søkersTidslinje = førskjøvetVilkårResultatTidslinjeMap[personopplysningGrunnlag.søker.aktør]!!

        val barna = personopplysningGrunnlag.barna.map { it.aktør }

        return barna.flatMap { barn ->
            val barnSinTidslinje = førskjøvetVilkårResultatTidslinjeMap[barn]!!
            val barnVilkårResultaterBådeBarnOgSøkerHarAlleOppfylt =
                barnSinTidslinje.kombinerMed(søkersTidslinje) { barnPeriode, søkerPeriode ->
                    søkerPeriode?.let { barnPeriode }
                }

            barnVilkårResultaterBådeBarnOgSøkerHarAlleOppfylt.tilPerioderIkkeNull().map { vilkårResultaterPeriode ->
                val erDeltBosted = vilkårResultaterPeriode.verdi.any {
                    it.vilkårType == Vilkår.BOR_MED_SØKER &&
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
                }

                val antallTimer = vilkårResultaterPeriode.verdi.single { it.vilkårType == Vilkår.BARNEHAGEPLASS }.antallTimer

                hentGyldigSatsFor(
                    antallTimer = antallTimer?.setScale(2, RoundingMode.HALF_UP),
                    erDeltBosted = erDeltBosted,
                    stønadFom = vilkårResultaterPeriode.fom!!.toYearMonth(),
                    stønadTom = vilkårResultaterPeriode.tom!!.toYearMonth()
                ).tilAndelTIlkjentYtelse(vilkårsvurdering.behandling.id, tilkjentYtelse, barn)
            }
        }
    }

    private fun SatsPeriode.tilAndelTIlkjentYtelse(
        behandlingId: Long,
        tilkjentYtelse: TilkjentYtelse,
        barn: Aktør
    ): AndelTilkjentYtelse {
        val kalkulertUtbetalingsbeløp = sats.prosent(prosent)
        return AndelTilkjentYtelse(
            behandlingId = behandlingId,
            tilkjentYtelse = tilkjentYtelse,
            aktør = barn,
            stønadFom = fom,
            stønadTom = tom,
            kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
            nasjonaltPeriodebeløp = kalkulertUtbetalingsbeløp,
            type = YtelseType.ORDINÆR_KONTANTSTØTTE,
            sats = sats,
            prosent = prosent
        )
    }

    private fun validerBeregnetPeriode(beløpsperiode: SatsPeriode, behandlingId: Long) {
        if (beløpsperiode.fom.isAfter(beløpsperiode.tom)) {
            throw Feil(
                "Feil i beregning for behandling $behandlingId," +
                    "fom ${beløpsperiode.fom} kan ikke være større enn tom ${beløpsperiode.tom}"
            )
        }
    }

    private fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
        andelTilkjentYtelserUtenEndringer: List<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer.map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it) }
        }
        // TODO - Endret Utbetaling Andel implementasjon kommer i neste levering
        return emptyList()
    }

    private fun beregnBeløpsperiode(
        overlappendePeriodeResultatSøker: PeriodeResultat,
        periodeResultatBarn: PeriodeResultat,
        perioderResultatForBarn: List<PeriodeResultat>,
        vilkårsvurdering: Vilkårsvurdering
    ): SatsPeriode {
        var oppfyltFom = maksimum(overlappendePeriodeResultatSøker.periodeFom, periodeResultatBarn.periodeFom)
        var oppfyltTom = minimum(overlappendePeriodeResultatSøker.periodeTom, periodeResultatBarn.periodeTom)

        val erDeltBosted = periodeResultatBarn.vilkårResultater.any {
            it.vilkårType == Vilkår.BOR_MED_SØKER &&
                it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
        }
        // Siden det slår sammen perioder tidligere, bør det ikke har flere barnehageplass vilkår i en periode
        val barnehageplassVilkår = periodeResultatBarn.vilkårResultater.first { it.vilkårType == Vilkår.BARNEHAGEPLASS }
        val antallTimer = barnehageplassVilkår.antallTimer

        when {
            perioderResultatForBarn.size == 1 -> {
                // når det finnes en IKKE_OPPFYLT periode etter, skal oppfyltTom ikke reduseres med en måned
                val harIkkeOppfyltPeriodeEtter =
                    vilkårsvurdering.personResultater.single { it.aktør == periodeResultatBarn.aktør }.vilkårResultater.any {
                        it.vilkårType == Vilkår.BARNEHAGEPLASS &&
                            it.resultat == Resultat.IKKE_OPPFYLT &&
                            it.periodeFom?.isAfter(oppfyltTom) == true
                    }
                // når det finnes en IKKE_OPPFYLT periode før, skal oppfyltFom ikke startes fra neste måned
                val harIkkeOppfyltPeriodeFør =
                    vilkårsvurdering.personResultater.single { it.aktør == periodeResultatBarn.aktør }.vilkårResultater.any {
                        it.vilkårType == Vilkår.BARNEHAGEPLASS &&
                            it.resultat == Resultat.IKKE_OPPFYLT &&
                            it.periodeTom?.isBefore(oppfyltFom) == true
                    }
                oppfyltFom = if (harIkkeOppfyltPeriodeFør) oppfyltFom else oppfyltFom.plusMonths(1)
                oppfyltTom = if (harIkkeOppfyltPeriodeEtter) oppfyltTom else oppfyltTom.minusMonths(1)
            }
            perioderResultatForBarn.size > 1 -> {
                // når det finnes 2 barnehageplass vilkår som er rett etter hverandre i månedsskifte,
                // skal oppfyltTom videreføres med en måned hvis det er reduksjon i barnehageplass
                val skalVidereføresEnMånedEkstra = perioderResultatForBarn.any {
                    val barnehageplassVilkårFraNestePeriode = it.vilkårResultater.first { vilkår ->
                        vilkår.vilkårType == Vilkår.BARNEHAGEPLASS
                    }
                    erBack2BackIMånedsskifte(
                        barnehageplassVilkår.periodeTom,
                        barnehageplassVilkårFraNestePeriode.periodeFom
                    ) && hentProsentForAntallTimer(barnehageplassVilkår.antallTimer) <
                        hentProsentForAntallTimer(barnehageplassVilkårFraNestePeriode.antallTimer)
                }
                // når det finnes 2 barnehageplass vilkår som er rett etter hverandre i månedsskifte,
                // skal oppfyltFom til neste periode starte måneden etter hvis det er reduksjon i barnehageplass
                val skalStarteNesteMåned = perioderResultatForBarn.any {
                    val barnehageplassVilkårFraForrigePeriode = it.vilkårResultater.first { vilkår ->
                        vilkår.vilkårType == Vilkår.BARNEHAGEPLASS
                    }
                    erBack2BackIMånedsskifte(
                        barnehageplassVilkårFraForrigePeriode.periodeTom,
                        barnehageplassVilkår.periodeFom
                    ) && hentProsentForAntallTimer(barnehageplassVilkår.antallTimer) >
                        hentProsentForAntallTimer(barnehageplassVilkårFraForrigePeriode.antallTimer)
                }

                // første perioder starter alltid måneden etter og siste periode slutter alltid måneden før
                val erFørstePeriode = perioderResultatForBarn.first().overlapper(periodeResultatBarn)
                val erSistePeriode = perioderResultatForBarn.last().overlapper(periodeResultatBarn)

                if (erFørstePeriode || skalStarteNesteMåned) oppfyltFom = oppfyltFom.plusMonths(1)
                oppfyltTom = when {
                    skalVidereføresEnMånedEkstra -> oppfyltTom.plusMonths(1)
                    erSistePeriode -> oppfyltTom.minusMonths(1)
                    else -> oppfyltTom
                }
            }
        }

        return hentGyldigSatsFor(
            antallTimer = antallTimer?.setScale(2, RoundingMode.HALF_UP),
            erDeltBosted = erDeltBosted,
            stønadFom = oppfyltFom.withDayOfMonth(1).toYearMonth(),
            stønadTom = oppfyltTom.sisteDagIMåned().toYearMonth()
        )
    }

    private fun maksimum(periodeFomSoker: LocalDate?, periodeFomBarn: LocalDate?): LocalDate {
        if (periodeFomSoker == null && periodeFomBarn == null) {
            throw Feil("Både søker og barn kan ikke ha null fom-dato")
        }
        return maxOf(periodeFomSoker ?: LocalDate.MIN, periodeFomBarn ?: LocalDate.MIN)
    }

    private fun minimum(periodeTomSoker: LocalDate?, periodeTomBarn: LocalDate?): LocalDate {
        if (periodeTomSoker == null && periodeTomBarn == null) {
            throw Feil("Både søker og barn kan ikke ha null i tom-dato")
        }
        return minOf(periodeTomBarn ?: LocalDate.MAX, periodeTomSoker ?: LocalDate.MAX)
    }

    private fun slåSammenPerioderForFlereAntallTimere(perioderResultaterForBarn: List<PeriodeResultat>): List<PeriodeResultat> {
        val periodeMedFlereAntallTimere = perioderResultaterForBarn.singleOrNull { periode ->
            periode.vilkårResultater.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }.size > 1
        } ?: return perioderResultaterForBarn

        val antallTimere = periodeMedFlereAntallTimere.vilkårResultater.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }
            .sortedBy { it.periodeFom }.map { it.antallTimer }

        // Det kan ikke være mer enn 2 antall timer i en periode siden KS ikke støtter mer enn 2 split i en måned ennå
        val kontantStøtteForFørstePeriode = hentProsentForAntallTimer(antallTimere.first())
        val kontantStøtteForSistePeriode = hentProsentForAntallTimer(antallTimere.last())

        val barnehageplassEndringstype = when {
            // ingen endring i utbetalingsprosent selv om antall timer kan være annerledes
            kontantStøtteForFørstePeriode == kontantStøtteForSistePeriode -> BarnehageplassEndringstype.INGEN_ENDRING
            // kontantStøtteForFørstePeriode er større enn kontantStøtteForSistePeriode
            // betyr utbetalingsprosent reduserer pga økning i barnehageplass
            kontantStøtteForFørstePeriode > kontantStøtteForSistePeriode -> BarnehageplassEndringstype.ØKNING
            // kontantStøtteForFørstePeriode er mindre enn kontantStøtteForSistePeriode
            // betyr utbetalingsprosent øker pga reduksjon i barnehageplass
            kontantStøtteForFørstePeriode < kontantStøtteForSistePeriode -> BarnehageplassEndringstype.REDUKSJON
            else -> BarnehageplassEndringstype.ØKNING
        }
        val oppdatertPerioderResultat = mutableListOf<PeriodeResultat>()
        when (barnehageplassEndringstype) {
            BarnehageplassEndringstype.INGEN_ENDRING -> {
                // når det er samme utbetalingsprosent, trenges det ikke en splitt. Da slås det perioder sammen
                oppdatertPerioderResultat.add(
                    PeriodeResultat(
                        aktør = perioderResultaterForBarn.first().aktør,
                        periodeFom = perioderResultaterForBarn.first().periodeFom,
                        periodeTom = perioderResultaterForBarn.last().periodeTom,
                        vilkårResultater = perioderResultaterForBarn.map { it.vilkårResultater }.flatten().toSet()
                    )
                )
            }
            BarnehageplassEndringstype.REDUKSJON -> {
                // for REDUKSJON, hentes det forrige periode og perioder slås sammen
                for (i in perioderResultaterForBarn.indices) {
                    if (perioderResultaterForBarn[i].overlapper(periodeMedFlereAntallTimere)) {
                        val sisteVerdi = if (i == 0) perioderResultaterForBarn[i] else perioderResultaterForBarn[i - 1]
                        oppdatertPerioderResultat.removeIf {
                            sisteVerdi.periodeFom == it.periodeFom &&
                                sisteVerdi.periodeTom == it.periodeTom
                        }
                        oppdatertPerioderResultat.add(
                            PeriodeResultat(
                                aktør = sisteVerdi.aktør,
                                periodeFom = sisteVerdi.periodeFom,
                                periodeTom = periodeMedFlereAntallTimere.periodeTom,
                                vilkårResultater = sisteVerdi.vilkårResultater
                            )
                        )
                    } else {
                        oppdatertPerioderResultat.add(perioderResultaterForBarn[i])
                    }
                }
            }
            BarnehageplassEndringstype.ØKNING -> {
                // for ØKNING hentes det neste periode og perioder slås sammen
                for (i in perioderResultaterForBarn.indices) {
                    if (perioderResultaterForBarn[i].overlapper(periodeMedFlereAntallTimere)) {
                        val nesteVerdi = if (i == 0) perioderResultaterForBarn[i] else perioderResultaterForBarn[i + 1]
                        oppdatertPerioderResultat.add(
                            PeriodeResultat(
                                aktør = nesteVerdi.aktør,
                                periodeFom = periodeMedFlereAntallTimere.periodeFom,
                                periodeTom = nesteVerdi.periodeTom,
                                vilkårResultater = nesteVerdi.vilkårResultater
                            )
                        )
                    } else {
                        if (oppdatertPerioderResultat.none { it.periodeTom == perioderResultaterForBarn[i].periodeTom }) {
                            oppdatertPerioderResultat.add(perioderResultaterForBarn[i])
                        }
                    }
                }
            }
        }
        return oppdatertPerioderResultat
    }
}
