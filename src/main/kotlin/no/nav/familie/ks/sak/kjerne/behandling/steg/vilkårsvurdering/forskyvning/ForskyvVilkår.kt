package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.ForskyvVilkårEtterFebruar2025
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.ForskyvVilkårFørFebruar2025
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

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

private fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeForPerson(
    person: Person,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = this.find { it.aktør == person.aktør }?.forskyvVilkårResultater() ?: emptyMap()

    return forskjøvedeVilkårResultater
        .map { it.value.tilTidslinje() }
        .kombiner { it.toList() }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
    person: Person,
    lovverk: Lovverk = Lovverk.FØR_LOVENDRING_2025,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = this.find { it.aktør == person.aktør }?.forskyvVilkårResultater(lovverk = lovverk) ?: emptyMap()

    return forskjøvedeVilkårResultater
        .map { it.value.tilTidslinje() }
        .kombiner { alleVilkårOppfyltEllerNull(it, person.type) }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

fun PersonResultat.forskyvVilkårResultater(
    lovverk: Lovverk = Lovverk.FØR_LOVENDRING_2025,
): Map<Vilkår, List<Periode<VilkårResultat>>> =
    when (lovverk) {
        Lovverk.FØR_LOVENDRING_2025 -> ForskyvVilkårFørFebruar2025.forskyvVilkårResultater(personResultat = this)
        Lovverk.LOVENDRING_FEBRUAR_2025 -> ForskyvVilkårEtterFebruar2025.forskyvVilkårResultater(this.vilkårResultater.toList())
    }

fun alleVilkårOppfyltEllerNull(
    vilkårResultater: Iterable<VilkårResultat?>,
    personType: PersonType,
    vilkårSomIkkeSkalSjekkesPå: List<Vilkår> = emptyList(),
): List<VilkårResultat>? {
    val skalHenteEøsSpesifikkeVilkår = vilkårResultater.any { it?.vurderesEtter == Regelverk.EØS_FORORDNINGEN && it.vilkårType == Vilkår.BOSATT_I_RIKET }
    val vilkårForPerson =
        Vilkår
            .hentVilkårFor(personType, skalHenteEøsSpesifikkeVilkår)
            .filter { it !in vilkårSomIkkeSkalSjekkesPå }
            .toSet()

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
