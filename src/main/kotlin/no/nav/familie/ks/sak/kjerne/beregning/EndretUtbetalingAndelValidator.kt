package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.erMellom
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.slåSammenOverlappendePerioder
import no.nav.familie.ks.sak.common.util.tilMånedPeriode
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.beregning.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal
import java.time.LocalDateTime

object EndretUtbetalingAndelValidator {

    fun validerPeriodeInnenforTilkjentYtelse(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: List<AndelTilkjentYtelse>
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()

        val feilMelding = "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for."
        val frontendFeilMelding = "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person " +
            "i hele eller deler av perioden."

        val minsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter { it.aktør == endretUtbetalingAndel.person?.aktør }
            .minByOrNull { it.stønadFom }?.stønadFom
            ?: throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)

        val størsteDatoForTilkjentYtelse = andelTilkjentYtelser.filter { it.aktør == endretUtbetalingAndel.person!!.aktør }
            .maxByOrNull { it.stønadTom }?.stønadTom
            ?: throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)

        if (checkNotNull(endretUtbetalingAndel.fom).isBefore(minsteDatoForTilkjentYtelse) ||
            checkNotNull(endretUtbetalingAndel.tom).isAfter(størsteDatoForTilkjentYtelse)
        ) {
            throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)
        }
    }

    fun validerÅrsak(årsak: Årsak?, endretUtbetalingAndel: EndretUtbetalingAndel, vilkårsvurdering: Vilkårsvurdering?) {
        checkNotNull(årsak) { "Årsak kan ikke være null" }
        when (årsak) {
            Årsak.DELT_BOSTED -> {
                val deltBostedPerioder = finnDeltBostedPerioder(
                    person = endretUtbetalingAndel.person,
                    vilkårsvurdering = vilkårsvurdering
                ).map { it.tilMånedPeriode() }
                validerDeltBosted(endretUtbetalingAndel = endretUtbetalingAndel, deltBostedPerioder = deltBostedPerioder)
            }
            Årsak.ETTERBETALING_3MND -> {
                validerEtterbetaling3Måned(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    kravDato = vilkårsvurdering?.behandling?.opprettetTidspunkt ?: LocalDateTime.now()
                )
            }
            else -> throw Feil("Årsak ${årsak.visningsnavn} er ikke implementert enda!!")
        }
    }

    internal fun finnDeltBostedPerioder(person: Person?, vilkårsvurdering: Vilkårsvurdering?): List<Periode> {
        if (vilkårsvurdering == null || person == null) return emptyList()
        val deltBostedPerioder = when (person.type) {
            PersonType.SØKER -> {
                // Endret Utbetaling med delt bosted kan pekes både på søker og barn selv om kun barnas vilkår settes til delt bosted
                // For søker, hentes delt bosted vilkårsresultater fra barnas vilkår
                // Hvis det er flere perioder(p.g.a flere barn), slås overlappende perioder sammen
                val deltBostedVilkårResultater = vilkårsvurdering.personResultater.flatMap { personResultat ->
                    personResultat.vilkårResultater.filter {
                        it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) &&
                            it.resultat == Resultat.OPPFYLT
                    }
                }

                val deltBostedPerioder = deltBostedVilkårResultater.groupBy { it.personResultat?.aktør }
                    .flatMap { (_, vilkårResultater) -> vilkårResultater.mapNotNull { it.tilPeriode(vilkår = vilkårResultater) } }

                slåSammenOverlappendePerioder(deltBostedPerioder.map { DatoIntervallEntitet(fom = it.fom, tom = it.tom) })
                    .filter { it.fom != null && it.tom != null }.map {
                        Periode(fom = checkNotNull(it.fom), tom = checkNotNull(it.tom))
                    }
            }
            else -> { // For barn, hentes det delt bosted for spesikt barn
                val personensVilkår = vilkårsvurdering.personResultater.single { it.aktør == person.aktør }
                val deltBostedVilkårResultater = personensVilkår.vilkårResultater.filter {
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && it.resultat == Resultat.OPPFYLT
                }

                deltBostedVilkårResultater.mapNotNull { it.tilPeriode(vilkår = deltBostedVilkårResultater) }
            }
        }
        // det slår sammen alle delt bosted perioder som er sammenhengende
        return slåSammenDeltBostedPerioderSomHengerSammen(perioder = deltBostedPerioder)
    }

    private fun slåSammenDeltBostedPerioderSomHengerSammen(perioder: List<Periode>): List<Periode> {
        if (perioder.isEmpty()) return emptyList()
        val sortertePerioder = perioder.sortedBy { it.fom }
        var periodenSomVises: Periode = sortertePerioder.first()
        val oppdatertListeMedPerioder = mutableListOf<Periode>()

        for (index in sortertePerioder.indices) {
            val periode = sortertePerioder[index]
            val nestePeriode = if (index == sortertePerioder.size - 1) null else sortertePerioder[index + 1]

            periodenSomVises = if (nestePeriode != null) {
                val andelerSkalSlåsSammen =
                    periode.tom.sisteDagIMåned().erDagenFør(nestePeriode.fom.førsteDagIInneværendeMåned())

                if (andelerSkalSlåsSammen) {
                    val nyPeriode = periodenSomVises.copy(tom = nestePeriode.tom)
                    nyPeriode
                } else {
                    oppdatertListeMedPerioder.add(periodenSomVises)
                    sortertePerioder[index + 1]
                }
            } else {
                oppdatertListeMedPerioder.add(periodenSomVises)
                break
            }
        }
        return oppdatertListeMedPerioder
    }

    private fun validerDeltBosted(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        deltBostedPerioder: List<MånedPeriode>
    ) {
        val fom = endretUtbetalingAndel.fom
        val tom = endretUtbetalingAndel.tom
        if (fom == null || tom == null) {
            throw FunksjonellFeil("Du må sette fom og tom.")
        }
        val endringsperiode = MånedPeriode(fom, tom)

        if (!deltBostedPerioder.any { endringsperiode.erMellom(MånedPeriode(fom = it.fom, tom = it.tom)) }) {
            throw FunksjonellFeil(
                melding = "Det finnes ingen delt bosted perioder i perioden det opprettes en endring med årsak delt bosted for.",
                frontendFeilmelding = "Du har valgt årsaken 'delt bosted', " +
                    "denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt."
            )
        }
    }

    private fun validerEtterbetaling3Måned(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        kravDato: LocalDateTime
    ) {
        if (endretUtbetalingAndel.prosent != BigDecimal.ZERO) {
            throw FunksjonellFeil(
                "Du kan ikke sette årsak etterbetaling 3 måned når du har valgt at perioden skal utbetales."
            )
        } else if (
            endretUtbetalingAndel.tom?.isAfter(kravDato.minusMonths(3).toLocalDate().toYearMonth()) == true
        ) {
            throw FunksjonellFeil(
                "Du kan ikke stoppe etterbetaling for en periode som ikke strekker seg mer enn 3 måned tilbake i tid."
            )
        }
    }
}
