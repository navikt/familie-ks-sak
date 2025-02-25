package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.tilDagMånedÅrKort
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.tilMånedÅrKort
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.tilTidslinje
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.ordinæreOgPraksisendringAndeler
import no.nav.familie.ks.sak.kjerne.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.UtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilPerioder
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.utfyltePerioder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

object OvergangsordningAndelValidator {
    fun validerOvergangsordningAndeler(
        overgangsordningAndeler: List<OvergangsordningAndel>,
        andelerTilkjentYtelseNåværendeBehandling: Set<AndelTilkjentYtelse>,
        andelerTilkjentYtelseForrigeBehandling: Set<AndelTilkjentYtelse>,
        personResultaterForBarn: List<PersonResultat>,
        barna: List<Person>,
    ) {
        val barnehageplassVilkårPerPerson = personResultaterForBarn.groupBy { it.aktør }.mapValues { it.value.flatMap { it.vilkårResultater.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS } } }
        validerAtAlleOpprettedeOvergangsordningAndelerErGyldigUtfylt(overgangsordningAndeler)
        validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler(andelerTilkjentYtelseNåværendeBehandling)
        validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder(overgangsordningAndeler.utfyltePerioder(), barnehageplassVilkårPerPerson)
        validerAndelerErIPeriodenBarnetEr20Til23Måneder(overgangsordningAndeler.utfyltePerioder())
        validerIngenEndringIOrdinæreAndelerTilkjentYtelse(andelerTilkjentYtelseNåværendeBehandling, andelerTilkjentYtelseForrigeBehandling, barna)
    }

    fun validerIngenEndringIOrdinæreAndelerTilkjentYtelse(
        andelerNåværendeBehandling: Set<AndelTilkjentYtelse>,
        andelerForrigeBehandling: Set<AndelTilkjentYtelse>,
        barna: List<Person>,
    ) {
        val nåværendeAndelerTidslinjer = andelerNåværendeBehandling.ordinæreOgPraksisendringAndeler().tilSeparateTidslinjerForBarna()
        val forrigeAndelerTidslinjer = andelerForrigeBehandling.ordinæreOgPraksisendringAndeler().tilSeparateTidslinjerForBarna()

        nåværendeAndelerTidslinjer
            .outerJoin(forrigeAndelerTidslinjer) { nåværendeAndeler, forrigeAndeler ->
                when {
                    nåværendeAndeler == null && forrigeAndeler == null -> false
                    nåværendeAndeler == null || forrigeAndeler == null -> true
                    else -> !nåværendeAndeler.innholdErLikt(forrigeAndeler)
                }
            }.forEach { (aktør, erEndringIOrdinæreAndelerTidslinje) ->
                erEndringIOrdinæreAndelerTidslinje.tilPerioder().forEach {
                    if (it.verdi == true) {
                        val fødselsdato = barna.first { it.aktør == aktør }.fødselsdato.tilKortString()
                        throw FunksjonellFeil(
                            melding = "Det er endringer i ordinære andeler for aktør med id ${aktør.aktørId} i behandling med årsak ${BehandlingÅrsak.OVERGANGSORDNING_2024}.",
                            frontendFeilmelding =
                                "Det er ikke mulig å gjøre andre endringer i behandlinger med årsak ${BehandlingÅrsak.OVERGANGSORDNING_2024.visningsnavn}. " +
                                    "Det er lagt inn endringer for barn født $fødselsdato i perioden ${it.fom?.tilDagMånedÅrKort()} til ${it.tom?.tilDagMånedÅrKort()}. " +
                                    "Slike endringer må gjøres i en egen revurderingsbehandling.",
                        )
                    }
                }
            }
    }

    fun validerAndelerErIPeriodenBarnetEr20Til23Måneder(
        overgangsordningAndeler: List<UtfyltOvergangsordningAndel>,
    ) {
        overgangsordningAndeler
            .groupBy { it.person }
            .forEach { (person, overgangsordningAndelerForPerson) ->
                val tidligsteGyldigeFom = beregnGyldigFom(person)
                val senesteGyldigeTom = beregnGyldigTom(person)
                overgangsordningAndelerForPerson.forEach {
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

    fun validerAtAlleOpprettedeOvergangsordningAndelerErGyldigUtfylt(overgangsordningAndeler: List<OvergangsordningAndel>) {
        runCatching {
            overgangsordningAndeler.forEach { it.validerAtObligatoriskeFelterErUtfylt() }
        }.onFailure {
            throw FunksjonellFeil(
                melding = "Det er opprettet instanser av OvergangsordningAndel som ikke er fylt ut før navigering til neste steg.",
                frontendFeilmelding =
                    "Du har opprettet en eller flere perioder for overgangsordning som er ufullstendig utfylt. " +
                        "Disse må enten fylles ut eller slettes før du kan gå videre.",
            )
        }

        overgangsordningAndeler.forEach { it.validerAtObligatoriskeFelterErGyldigUtfylt() }
    }

    fun validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler(andelerTilkjentYtelse: Set<AndelTilkjentYtelse>) {
        val ordinæreAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.ORDINÆR_KONTANTSTØTTE }
        andelerTilkjentYtelse.filter { it.type == YtelseType.OVERGANGSORDNING }.forEach { overgangsordningAndel ->
            if (ordinæreAndeler.any {
                    it.overlapperPeriode(overgangsordningAndel.periode) && it.aktør == overgangsordningAndel.aktør
                }
            ) {
                throw FunksjonellFeil("Perioder for overgangsordning kan ikke overlappe med perioder med ordinær utbetaling for barn født ${overgangsordningAndel.aktør}.")
            }
        }
    }

    fun validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder(
        overgangsordningAndeler: List<UtfyltOvergangsordningAndel>,
        barnehageplassVilkårPerPerson: Map<Aktør, List<VilkårResultat>>,
    ) {
        overgangsordningAndeler.forEach { overgangsordningAndel ->
            val barnehagevilkår =
                barnehageplassVilkårPerPerson[overgangsordningAndel.person.aktør]
                    ?: throw FunksjonellFeil("Fant ikke barnehagevilkår for barn født ${overgangsordningAndel.person.fødselsdato.tilKortString()}")
            validerAtBarnehagevilkårErOppfyltIOvergangsordningAndelPeriode(
                overgangsordningAndel = overgangsordningAndel,
                barnehageplassVilkår = barnehagevilkår,
            )
        }
    }

    fun validerIngenOverlappMedEksisterendeOvergangsordningAndeler(
        nyOvergangsordningAndel: UtfyltOvergangsordningAndel,
        eksisterendeUtfylteOvergangsordningAndeler: List<UtfyltOvergangsordningAndel>,
    ) {
        if (eksisterendeUtfylteOvergangsordningAndeler.any {
                it.overlapperMed(nyOvergangsordningAndel) && it.person == nyOvergangsordningAndel.person
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
                "Barnehageplassvilkåret må være oppfylt alle periodene det er overgangsordning for barn født ${overgangsordningAndel.person.fødselsdato.tilKortString()}. " +
                    "Vilkåret er ikke oppfylt i " +
                    when (perioderBarnehagevilkårIkkeErOppfylt.size) {
                        1 -> "perioden ${perioderBarnehagevilkårIkkeErOppfylt.first()}."
                        else -> "periodene ${perioderBarnehagevilkårIkkeErOppfylt.dropLast(1).joinToString()} og ${perioderBarnehagevilkårIkkeErOppfylt.last()}."
                    }
            throw FunksjonellFeil(
                melding = "Barnehageplassvilkåret er ikke oppfylt i perioden som blir forsøkt lagt til.",
                frontendFeilmelding = feilmelding,
            )
        }
    }

    fun validerFomDato(
        overgangsordningAndel: UtfyltOvergangsordningAndel,
        gyldigFom: YearMonth,
    ) {
        if (overgangsordningAndel.fom.isBefore(gyldigFom)) {
            throw FunksjonellFeil(
                "F.o.m. dato (${overgangsordningAndel.fom.tilMånedÅrKort()}) kan ikke være tidligere enn barnet fyller 20 måneder (${gyldigFom.tilMånedÅrKort()})",
            )
        }
    }

    fun validerTomDato(
        overgangsordningAndel: UtfyltOvergangsordningAndel,
        gyldigTom: YearMonth,
    ) {
        if (overgangsordningAndel.tom.isAfter(gyldigTom)) {
            throw FunksjonellFeil(
                "T.o.m. dato (${overgangsordningAndel.tom.tilMånedÅrKort()}) kan ikke være senere enn barnet fyller 23 måneder (${gyldigTom.tilMånedÅrKort()})",
            )
        }
    }
}
