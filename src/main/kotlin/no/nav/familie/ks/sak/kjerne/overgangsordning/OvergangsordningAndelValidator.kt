package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.tilDagMånedÅrKort
import no.nav.familie.ks.sak.common.util.tilMånedÅrKort
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.ordinæreAndeler
import no.nav.familie.ks.sak.kjerne.beregning.domene.overgangsordningAndeler
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.UtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilPerioder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.YearMonth

object OvergangsordningAndelValidator {
    fun validerAndelerErIPeriodenBarnetEr20Til23Måneder(
        andeler: List<UtfyltOvergangsordningAndel>,
    ) {
        andeler
            .groupBy { it.person }
            .forEach { (person, andelerForPerson) ->
                val tidligsteGyldigeFom = beregnGyldigFom(person)
                val senesteGyldigeTom = beregnGyldigTom(person)
                andelerForPerson.forEach {
                    if (it.fom.isBefore(tidligsteGyldigeFom) || it.tom.isAfter(senesteGyldigeTom)) {
                        throw FunksjonellFeil(
                            melding = "Perioden som blir forsøkt lagt til er utenfor gyldig periode for person.",
                            frontendFeilmelding =
                                "Perioden for overgangsordning du forsøker å legge til (${it.fom} - ${it.tom}) er utenfor gyldig periode for barn født ${person.fødselsdato}. " +
                                    "Gyldig periode er fra og med 20 til og med 23 måneder etter fødselsdato ($tidligsteGyldigeFom - $senesteGyldigeTom).",
                        )
                    }
                }
            }
    }

    fun validerAtAlleOpprettedeOvergangsordningAndelerErUtfylt(andeler: List<OvergangsordningAndel>) {
        runCatching {
            andeler.forEach { it.validerAlleFelterUtfylt() }
        }.onFailure {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av OvergangsordningAndel som ikke er fylt ut før navigering til neste steg.",
                frontendFeilmelding =
                    "Du har opprettet en eller flere perioder for overgangsordning som er ufullstendig utfylt. " +
                        "Disse må enten fylles ut eller slettes før du kan gå videre.",
            )
        }
    }

    fun validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler(tilkjentYtelse: TilkjentYtelse) {
        val ordinæreAndeler = tilkjentYtelse.ordinæreAndeler()
        tilkjentYtelse.overgangsordningAndeler().forEach { overgangsordningAndel ->
            if (ordinæreAndeler.any {
                    it.overlapperPeriode(overgangsordningAndel.periode) && it.aktør == overgangsordningAndel.aktør
                }
            ) {
                throw FunksjonellFeil("Perioder for overgangsordning kan ikke overlappe med perioder med ordinær utbetaling for barnet.")
            }
        }
    }

    fun validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder(
        andeler: List<UtfyltOvergangsordningAndel>,
        barnehageplassVilkårPerPerson: Map<Aktør, List<VilkårResultat>>,
    ) {
        val andelerTidslinjePerAktør = andeler.groupBy { it.person.aktør }.mapValues { it.value.tilPerioder().tilTidslinje() }
        val barnehagevilkårTidslinjePerAktør = barnehageplassVilkårPerPerson.mapValues { it.value.tilTidslinje() }

        andelerTidslinjePerAktør.forEach { (aktør, andelerTidslinje) ->
            val barnehagevilkårTidslinje = barnehagevilkårTidslinjePerAktør[aktør] ?: throw FunksjonellFeil("Fant ikke barnehagevilkår for aktør $aktør")
            andelerTidslinje.kombinerMed(barnehagevilkårTidslinje) { andel, vilkår ->
                if (andel != null && vilkår?.resultat != Resultat.OPPFYLT) {
                    throw FunksjonellFeil(
                        melding = "Barnehagevilkåret må være oppfylt for alle periodene du prøver å legge til periode for overgangsordning.",
                        frontendFeilmelding = "Barnehagevilkåret for barnet må være oppfylt for alle periodene det er overgangsordning.",
                    )
                }
            }
        }
    }

    fun validerIngenOverlappMedEksisterendeOvergangsordningAndeler(
        nyAndel: UtfyltOvergangsordningAndel,
        eksisterendeAndeler: List<OvergangsordningAndel>,
    ) {
        if (eksisterendeAndeler.any {
                it.overlapperMed(nyAndel.periode) &&
                    it.person == nyAndel.person
            }
        ) {
            throw FunksjonellFeil(
                melding = "Perioden som blir forsøkt lagt til overlapper med eksisterende periode for overgangsordning på person.",
                frontendFeilmelding = "Perioden du forsøker å legge til overlapper med eksisterende periode for overgangsordning på personen.",
            )
        }
    }

    fun validerAtBarnehagevilkårErOppfyltIOvergangsordningAndelPeriode(
        overgangsordningAndel: UtfyltOvergangsordningAndel,
        barnehageplassVilkår: List<VilkårResultat>,
    ) {
        val barnehagevilkårTidslinje = barnehageplassVilkår.tilTidslinje()
        val andelTidslinje = listOf(overgangsordningAndel).tilPerioder().tilTidslinje()

        val perioderBarnehagevilkårIkkeErOppfylt =
            barnehagevilkårTidslinje
                .kombinerMed(andelTidslinje) { vilkår, andel -> andel != null && vilkår?.resultat != Resultat.OPPFYLT }
                .tilPerioder()
                .filtrerIkkeNull()
                .filter { it.verdi }
                .map { "${it.fom?.tilDagMånedÅrKort()} til ${it.tom?.tilDagMånedÅrKort()}" }

        if (perioderBarnehagevilkårIkkeErOppfylt.isNotEmpty()) {
            val feilmelding =
                "Barnehagevilkåret må være oppfylt alle periodene det er overgangsordning. " +
                    "Vilkåret er ikke oppfylt i " +
                    when (perioderBarnehagevilkårIkkeErOppfylt.size) {
                        1 -> "perioden ${perioderBarnehagevilkårIkkeErOppfylt.first()}."
                        else -> "periodene ${perioderBarnehagevilkårIkkeErOppfylt.dropLast(1).joinToString()} og ${perioderBarnehagevilkårIkkeErOppfylt.last()}."
                    }
            throw FunksjonellFeil(
                melding = "Barnehagevilkåret er ikke oppfylt i perioden som blir forsøkt lagt til.",
                frontendFeilmelding = feilmelding,
            )
        }
    }

    fun validerFomDato(
        andel: UtfyltOvergangsordningAndel,
        gyldigFom: YearMonth,
    ) {
        if (andel.fom.isBefore(gyldigFom)) {
            throw FunksjonellFeil(
                "F.o.m. dato (${andel.fom.tilMånedÅrKort()}) kan ikke være tidligere enn barnet fyller 20 måneder (${gyldigFom.tilMånedÅrKort()})",
            )
        }
    }

    fun validerTomDato(
        andel: UtfyltOvergangsordningAndel,
        gyldigTom: YearMonth,
    ) {
        if (andel.tom.isAfter(gyldigTom)) {
            throw FunksjonellFeil(
                "T.o.m. dato (${andel.tom.tilMånedÅrKort()}) kan ikke være senere enn barnet fyller 23 måneder (${gyldigTom.tilMånedÅrKort()})",
            )
        }
    }
}
