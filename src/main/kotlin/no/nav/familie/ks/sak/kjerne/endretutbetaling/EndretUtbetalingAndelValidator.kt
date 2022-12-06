package no.nav.familie.ks.sak.kjerne.endretutbetaling

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Verdi
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.slåSammenLikeTidslinjer
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.erMellom
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

object EndretUtbetalingAndelValidator {

    fun validerPeriodeInnenforTilkjentYtelse(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        andelTilkjentYtelser: List<AndelTilkjentYtelse>
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()

        val feilMelding = "Det er ingen tilkjent ytelse for personen det blir forsøkt lagt til en endret periode for."
        val frontendFeilMelding = "Du har valgt en periode der det ikke finnes tilkjent ytelse for valgt person " +
            "i hele eller deler av perioden."

        val minsteDatoForTilkjentYtelse =
            andelTilkjentYtelser.filter { it.aktør == endretUtbetalingAndel.person?.aktør }
                .minByOrNull { it.stønadFom }?.stønadFom
                ?: throw FunksjonellFeil(melding = feilMelding, frontendFeilmelding = frontendFeilMelding)

        val størsteDatoForTilkjentYtelse =
            andelTilkjentYtelser.filter { it.aktør == endretUtbetalingAndel.person!!.aktør }
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
                )
                validerDeltBosted(
                    endretUtbetalingAndel = endretUtbetalingAndel,
                    deltBostedPerioder = deltBostedPerioder
                )
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

    fun validerAtAlleOpprettedeEndringerErUtfylt(endretUtbetalingAndeler: List<EndretUtbetalingAndel>) {
        runCatching {
            endretUtbetalingAndeler.forEach { it.validerUtfyltEndring() }
        }.onFailure {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er fylt ut før navigering til neste steg.",
                frontendFeilmelding = "Du har opprettet en eller flere endrede utbetalingsperioder " +
                    "som er ufullstendig utfylt. Disse må enten fylles ut eller slettes før du kan gå videre."
            )
        }
    }

    fun validerAtEndringerErTilknyttetAndelTilkjentYtelse(endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>) {
        if (endretUtbetalingAndeler.any { it.andelerTilkjentYtelse.isEmpty() }) {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av EndretUtbetalingandel som ikke er tilknyttet noen andeler. " +
                    "De må enten lagres eller slettes av SB.",
                frontendFeilmelding = "Du har endrede utbetalingsperioder. Bekreft, slett eller oppdater periodene i listen."
            )
        }
    }

    /**
     * Funksjon som finner delt bosted perioder for spesifikk person
     * @param[person] Person kan enten være SØKER eller BARN
     * @param[vilkårsvurdering] Vilkårsvurdering for å finne ut hvilke perioder har DELT_BOSTED
     * @return List<Periode<Long>> returnerer delt bosted perioder hvor Periode har behandlingId som verdi*/
    fun finnDeltBostedPerioder(person: Person?, vilkårsvurdering: Vilkårsvurdering?): List<Periode<Long>> {
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

                // slår sammen overlappende perioder
                deltBostedPerioder.filter { it.fom != null && it.tom != null }.map { listOf(it).tilTidslinje() }
                    .slåSammenLikeTidslinjer { _, _ -> Verdi(person.personopplysningGrunnlag.behandlingId) }
                    .slåSammenLikePerioder().tilPerioder()
            }

            else -> { // For barn, hentes det delt bosted for spesikt barn
                val personensVilkår = vilkårsvurdering.personResultater.single { it.aktør == person.aktør }
                val deltBostedVilkårResultater = personensVilkår.vilkårResultater.filter {
                    it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && it.resultat == Resultat.OPPFYLT
                }

                deltBostedVilkårResultater.mapNotNull { it.tilPeriode(vilkår = deltBostedVilkårResultater) }
                    .tilTidslinje().tilPerioder()
            }
        }
        // det slår sammen alle delt bosted perioder som er sammenhengende
        return deltBostedPerioder.tilTidslinje().slåSammenLikePerioder().tilPerioder().filtrerIkkeNull()
    }

    private fun validerDeltBosted(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        deltBostedPerioder: List<Periode<Long>>
    ) {
        val fom = endretUtbetalingAndel.fom
        val tom = endretUtbetalingAndel.tom
        if (fom == null || tom == null) {
            throw FunksjonellFeil("Du må sette fom og tom.")
        }
        val endringsperiode = MånedPeriode(fom, tom)

        if (!deltBostedPerioder.any {
            endringsperiode.erMellom(
                    MånedPeriode(
                            fom = checkNotNull(it.fom).toYearMonth(),
                            tom = checkNotNull(it.tom).toYearMonth()
                        )
                )
        }
        ) {
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

    fun validerUtbetalingMotÅrsak(årsak: Årsak?, skalUtbetales: Boolean) {
        if (skalUtbetales && (årsak == Årsak.ENDRE_MOTTAKER || årsak == Årsak.ALLEREDE_UTBETALT)) {
            val feilmelding = "Du kan ikke velge denne årsaken og si at kontantstøtten skal utbetales."
            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }

    fun validerTomDato(tomDato: YearMonth?, gyldigTomEtterDagensDato: YearMonth?, årsak: Årsak?) {
        val dagensDato = YearMonth.now()
        if (årsak == Årsak.ALLEREDE_UTBETALT && tomDato?.isAfter(dagensDato) == true) {
            val feilmelding =
                "For årsak '${årsak.visningsnavn}' kan du ikke legge inn til og med dato som er i neste måned eller senere."

            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
        if (tomDato?.isAfter(dagensDato) == true && tomDato != gyldigTomEtterDagensDato) {
            val feilmelding =
                "Du kan ikke legge inn til og med dato som er i neste måned eller senere. Om det gjelder en løpende periode vil systemet legge inn riktig dato for deg."

            throw FunksjonellFeil(frontendFeilmelding = feilmelding, melding = feilmelding)
        }
    }

    fun validerIngenOverlappendeEndring(
        endretUtbetalingAndel: EndretUtbetalingAndel,
        eksisterendeEndringerPåBehandling: List<EndretUtbetalingAndel>
    ) {
        endretUtbetalingAndel.validerUtfyltEndring()
        if (eksisterendeEndringerPåBehandling.any
            {
                it.overlapperMed(endretUtbetalingAndel.periode) &&
                    it.person == endretUtbetalingAndel.person &&
                    it.årsak == endretUtbetalingAndel.årsak
            }
        ) {
            throw FunksjonellFeil(
                melding = "Perioden som blir forsøkt lagt til overlapper med eksisterende periode på person.",
                frontendFeilmelding = "Perioden du forsøker å legge til overlapper med eksisterende periode på personen. Om dette er ønskelig må du først endre den eksisterende perioden."
            )
        }
    }
}
