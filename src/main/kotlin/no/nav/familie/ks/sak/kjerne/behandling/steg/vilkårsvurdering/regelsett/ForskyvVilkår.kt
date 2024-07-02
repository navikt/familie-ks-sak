package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.klipp
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.DATO_LOVENDRING_2024
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.forskyvEtterLovgivning2021
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.forskyvEtterLovgivning2024
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

fun Collection<PersonResultat>.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    erToggleForLovendringAugust2024På: Boolean,
): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(person, erToggleForLovendringAugust2024På),
        )
    }

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeMap(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    erToggleForLovendringAugust2024På: Boolean,
): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilForskjøvetVilkårResultatTidslinjeForPerson(person, erToggleForLovendringAugust2024På),
        )
    }

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeForPerson(
    person: Person,
    erToggleForLovendringAugust2024På: Boolean,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = forskyvVilkårResultaterForPerson(person, erToggleForLovendringAugust2024På)

    return forskjøvedeVilkårResultater
        .kombiner { it.toList() }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

private fun Collection<PersonResultat>.forskyvVilkårResultaterForPerson(
    person: Person,
    erToggleForLovendringAugust2024På: Boolean,
): List<Tidslinje<VilkårResultat>> {
    val personResultat = this.find { it.aktør == person.aktør }

    val vilkårResultaterForAktør = personResultat?.vilkårResultater ?: emptyList()

    val vilkårResultaterForAktørMap =
        vilkårResultaterForAktør
            .groupByTo(mutableMapOf()) { it.vilkårType }
            .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

    val forskjøvedeVilkårResultater =
        vilkårResultaterForAktørMap.map { (vilkårType, vilkårResultater) ->
            forskyvVilkårResultater(vilkårType, vilkårResultater, erToggleForLovendringAugust2024På).tilTidslinje()
        }
    return forskjøvedeVilkårResultater
}

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
    person: Person,
    erToggleForLovendringAugust2024På: Boolean,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = forskyvVilkårResultaterForPerson(person, erToggleForLovendringAugust2024På)

    return forskjøvedeVilkårResultater
        .kombiner { alleVilkårOppfyltEllerNull(it, person.type) }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

fun forskyvVilkårResultater(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>,
    erToggleForLovendringAugust2024På: Boolean,
): List<Periode<VilkårResultat>> {
    val forskjøvetVilkårResultaterTidslinje2021 = forskyvEtterLovgivning2021(vilkårType, vilkårResultater).tilTidslinje()

    if (!erToggleForLovendringAugust2024På) {
        return forskjøvetVilkårResultaterTidslinje2021.tilPerioderIkkeNull()
    }
    val forskjøvetVilkårResultaterTidslinje2024 = forskyvEtterLovgivning2024(vilkårType, vilkårResultater).tilTidslinje()

    val klippetTidslinje2021 =
        forskjøvetVilkårResultaterTidslinje2021.klipp(
            startsTidspunkt = forskjøvetVilkårResultaterTidslinje2021.startsTidspunkt,
            sluttTidspunkt = DATO_LOVENDRING_2024.minusDays(1),
        )

    val klippetTidslinje2024 =
        forskjøvetVilkårResultaterTidslinje2024.klipp(
            startsTidspunkt = DATO_LOVENDRING_2024,
            sluttTidspunkt = TIDENES_ENDE,
        )

    return klippetTidslinje2021
        .kombinerMed(klippetTidslinje2024) { vilkår2021, vilkår2024 -> vilkår2021 ?: vilkår2024 }
        .tilPerioderIkkeNull()
}

private fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
    if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

private fun alleVilkårOppfyltEllerNull(
    vilkårResultater: Iterable<VilkårResultat?>,
    personType: PersonType,
): List<VilkårResultat>? {
    val skalHenteEøsSpesifikkeVilkår = vilkårResultater.any { it?.vurderesEtter == Regelverk.EØS_FORORDNINGEN && it.vilkårType == Vilkår.BOSATT_I_RIKET }
    val vilkårForPerson = Vilkår.hentVilkårFor(personType, skalHenteEøsSpesifikkeVilkår)

    return if (erAlleVilkårForPersonEntenOppfyltEllerIkkeAktuelt(vilkårForPerson, vilkårResultater)) {
        vilkårResultater.filterNotNull()
    } else {
        null
    }
}

private fun erAlleVilkårForPersonEntenOppfyltEllerIkkeAktuelt(
    vilkårForPerson: Set<Vilkår>,
    vilkårResultater: Iterable<VilkårResultat?>,
) = vilkårForPerson.all { vilkår ->
    vilkårResultater.any {
        val erOppfyltEllerIkkeAktuelt = it?.resultat == Resultat.OPPFYLT || it?.resultat == Resultat.IKKE_AKTUELT

        erOppfyltEllerIkkeAktuelt && it?.vilkårType == vilkår
    }
}
