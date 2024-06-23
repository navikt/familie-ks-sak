package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.klipp
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
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

private val DATO_FOR_LOVENDRING_AV_FORSKYVNINGER: LocalDate = LocalDate.of(2024, 8, 1)

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
    val forskjøvetVilkårResultaterEtterLovgivning2021 =
        forskyvEtterLovgivning2021(
            vilkårType,
            vilkårResultater,
        )

    val forskjøvetVilkårResultaterEtterLovgivning2024 =
        forskyvEtterLovgivning2024(
            vilkårType,
            vilkårResultater,
        )

    val forskjøvetVilkårResultaterTidslinje2021 = forskjøvetVilkårResultaterEtterLovgivning2021.tilTidslinje()

    val forskjøvetVilkårResultaterTidslinje2024 = forskjøvetVilkårResultaterEtterLovgivning2024.tilTidslinje()

    val klippetTidslinje2021 =
        forskjøvetVilkårResultaterTidslinje2021.klipp(
            forskjøvetVilkårResultaterTidslinje2021.startsTidspunkt,
            DATO_FOR_LOVENDRING_AV_FORSKYVNINGER.minusDays(1),
        )

    val klippetTidslinje2024 =
        forskjøvetVilkårResultaterTidslinje2024.klipp(
            DATO_FOR_LOVENDRING_AV_FORSKYVNINGER,
            forskjøvetVilkårResultaterTidslinje2024.kalkulerSluttTidspunkt(),
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
