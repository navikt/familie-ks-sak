package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.klipp
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
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
import java.time.LocalDate

fun Collection<PersonResultat>.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(person),
        )
    }

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeMap(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilForskjøvetVilkårResultatTidslinjeForPerson(person),
        )
    }

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeForPerson(
    person: Person,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = forskyvVilkårResultaterForPerson(person)

    return forskjøvedeVilkårResultater
        .kombiner { it.toList() }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

private fun Collection<PersonResultat>.forskyvVilkårResultaterForPerson(
    person: Person,
): List<Tidslinje<VilkårResultat>> {
    val personResultat = this.find { it.aktør == person.aktør }

    val vilkårResultaterForAktør = personResultat?.vilkårResultater ?: emptyList()

    val vilkårResultaterForAktørMap =
        vilkårResultaterForAktør
            .groupByTo(mutableMapOf()) { it.vilkårType }
            .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

    val forskjøvedeVilkårResultater =
        vilkårResultaterForAktørMap.map { (vilkårType, vilkårResultater) ->
            forskyvVilkårResultater(vilkårType, vilkårResultater).tilTidslinje()
        }
    return forskjøvedeVilkårResultater
}

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
    person: Person,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = forskyvVilkårResultaterForPerson(person)

    return forskjøvedeVilkårResultater
        .kombiner { alleVilkårOppfyltEllerNull(it, person.type) }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

fun forskyvVilkårResultater(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>,
): List<Periode<VilkårResultat>> {

    val tidslinje2021 = forskyvEtterLovgivning2021(vilkårType, vilkårResultater).tilTidslinje()
    val tidslinje2024 = forskyvEtterLovgivning2024(vilkårType, vilkårResultater).tilTidslinje()

    val sisteDatoIJuli = LocalDate.of(2024, 7, 31)
    val førsteAugust = LocalDate.of(2024, 8, 1)

    val klippetTidslinje2021 = tidslinje2021.klipp(TIDENES_MORGEN, sisteDatoIJuli)
    val klippetTidslinje2024 = tidslinje2024.klipp(førsteAugust, TIDENES_ENDE)

    val tilPerioderIkkeNull = klippetTidslinje2021.kombinerMed(klippetTidslinje2024) { vilkår2021, vilkår2024 ->
        if (vilkår2021 != null) {
            vilkår2021
        } else {
            vilkår2024
        }
    }.tilPerioderIkkeNull()

    return tilPerioderIkkeNull

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
