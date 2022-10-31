package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.erSammeEllerEtter
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.hentInnvilgedePerioder
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.SatsPeriode
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentGyldigSatsFor
import no.nav.familie.ks.sak.kjerne.beregning.domene.prosent
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
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

    fun beregnAndelerTilkjentYtelseForBarna(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        vilkårsvurdering: Vilkårsvurdering,
        tilkjentYtelse: TilkjentYtelse
    ): List<AndelTilkjentYtelse> {
        val barnaIdenter = personopplysningGrunnlag.barna.associateBy { it.aktør.aktørId }
        val (innvilgedePeriodeResultaterSøker, innvilgedePeriodeResultaterBarna) = hentInnvilgedePerioder(
            personopplysningGrunnlag,
            vilkårsvurdering
        )
        val relevanteSøkerPerioder = innvilgedePeriodeResultaterSøker
            .filter { søkersperiode -> innvilgedePeriodeResultaterBarna.any { søkersperiode.overlapper(it) } }

        return innvilgedePeriodeResultaterBarna.flatMap { periodeResultatBarn ->
            relevanteSøkerPerioder.filter { it.overlapper(periodeResultatBarn) }
                .map { overlappendePeriodeResultatSøker ->
                    val barn = barnaIdenter[periodeResultatBarn.aktør.aktørId] ?: throw Feil("Finner ikke barn")
                    // beregn beløpsperiode med sats og tilsvarende gjeledende prosentandel
                    val beløpsperiode = beregnBeløpsperiode(
                        overlappendePeriodeResultatSøker,
                        periodeResultatBarn,
                        innvilgedePeriodeResultaterSøker,
                        innvilgedePeriodeResultaterBarna,
                        barn
                    )
                    // beregn utbetalingsbeløp basert på sats og prosentandel og Opprett AndelTilkjentYtelse
                    val kalkulertUtbetalingsbeløp = beløpsperiode.sats.prosent(beløpsperiode.prosent)
                    AndelTilkjentYtelse(
                        behandlingId = vilkårsvurdering.behandling.id,
                        tilkjentYtelse = tilkjentYtelse,
                        aktør = barn.aktør,
                        stønadFom = beløpsperiode.fom,
                        stønadTom = beløpsperiode.tom,
                        kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
                        nasjonaltPeriodebeløp = kalkulertUtbetalingsbeløp,
                        type = YtelseType.ORDINÆR_KONTANTSTØTTE,
                        sats = beløpsperiode.sats,
                        prosent = beløpsperiode.prosent
                    )
                }
        }
    }

    fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
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
        innvilgedePeriodeResultatBarna: List<PeriodeResultat>,
        innvilgetPeriodeResultatSøker: List<PeriodeResultat>,
        barn: Person
    ): SatsPeriode {
        val oppfyltFom = maksimum(overlappendePeriodeResultatSøker.periodeFom, periodeResultatBarn.periodeFom)
        val oppfyltTom = minimum(overlappendePeriodeResultatSøker.periodeTom, periodeResultatBarn.periodeTom)

        val periodeTomFraMellom1og2ÅrEllerAdoptertVilkår = periodeResultatBarn.vilkårResultater
            .find { it.vilkårType == Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT }?.periodeTom
            ?: throw Feil("periode tom kan ikke være null for Vilkår MELLOM_1_OG_2_ELLER_ADOPTERT")

        val skalAvsluttesMånedenFør =
            when {
                barn.erDød() -> {
                    checkNotNull(barn.dødsfall).dødsfallDato.førsteDagIInneværendeMåned().erSammeEllerEtter(
                        periodeTomFraMellom1og2ÅrEllerAdoptertVilkår.førsteDagIInneværendeMåned()
                    )
                }
                else -> {
                    oppfyltTom == periodeTomFraMellom1og2ÅrEllerAdoptertVilkår
                }
            }
        val erDeltBosted = periodeResultatBarn.vilkårResultater.any {
            it.vilkårType == Vilkår.BOR_MED_SØKER &&
                it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
        }
        val antallTimer = periodeResultatBarn.vilkårResultater.find { it.vilkårType == Vilkår.BARNEHAGEPLASS }?.antallTimer

        return hentGyldigSatsFor(
            antallTimer = antallTimer,
            erDeltBosted = erDeltBosted,
            stønadFom = oppfyltFom.plusMonths(1).withDayOfMonth(1).toYearMonth(),
            stønadTom = oppfyltTom.minusMonths(1).sisteDagIMåned().toYearMonth()
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
}
