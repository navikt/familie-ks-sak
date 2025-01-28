package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.ForskyvVilkårFørFebruar2025
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.lovverk.LovverkUtleder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
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
        Lovverk.LOVENDRING_FEBRUAR_2025 -> TODO()
    }

fun PersonResultat.forskyvVilkårResultaterEtterLovverkTidslinje(lovverkTidslinje: Tidslinje<Lovverk>): Map<Vilkår, List<Periode<VilkårResultat>>> {
    val forskjøvedeVilkårResultaterEtterMuligeLovverk =
        lovverkTidslinje
            .tilPerioderIkkeNull()
            .map { it.verdi }
            .toSet()
            .map { forskyvningsLovverk ->
                this
                    .forskyvVilkårResultater(forskyvningsLovverk)
                    .mapValues {
                        it.value
                            .tilTidslinje()
                            .kombinerMed(lovverkTidslinje) { vilkårResultat, lovverk ->
                                if (forskyvningsLovverk == lovverk) {
                                    vilkårResultat
                                } else {
                                    null
                                }
                            }.tilPerioderIkkeNull()
                    }
            }

    return forskjøvedeVilkårResultaterEtterMuligeLovverk
        .flatMap { it.keys }
        .associateWith { key ->
            forskjøvedeVilkårResultaterEtterMuligeLovverk
                .flatMap { it.getOrDefault(key, emptyList()) }
        }
}

fun Collection<PersonResultat>.forskyvVilkårResultater(personopplysningGrunnlag: PersonopplysningGrunnlag): Map<Aktør, Map<Vilkår, List<Periode<VilkårResultat>>>> {
    val barnasForskjøvedeVilkårResultater =
        this.filter { !it.erSøkersResultater() }.associate { personResultat ->
            val lovverk =
                LovverkUtleder.utledLovverkForBarn(
                    personopplysningGrunnlag.barna.single { it.aktør == personResultat.aktør }.fødselsdato,
                    skalBestemmeLovverkBasertPåFødselsdato = false,
                )
            personResultat.aktør to personResultat.forskyvVilkårResultater(lovverk)
        }

    // Lager LovverkTidslinje basert på barnas forskjøvede vilkårResultater
    val barnasLovverkTidslinje =
        barnasForskjøvedeVilkårResultater
            .map { entry ->
                entry.value.tilLovverkTidslinje(personopplysningGrunnlag.barna.single { it.aktør == entry.key })
            }.kombiner {
                val lovverk = it.toSet()
                if (lovverk.size > 1) {
                    throw Feil("Støtter ikke overlappende lovverk")
                }
                lovverk.single()
            }

    // Forskyver søker etter alle lovverk og kombinerer med lovverktidslinje
    val søkersForskjøvedeVilkårResultater = this.single { it.erSøkersResultater() }.forskyvVilkårResultaterEtterLovverkTidslinje(barnasLovverkTidslinje)

    return barnasForskjøvedeVilkårResultater.plus(Pair(this.single { it.erSøkersResultater() }.aktør, søkersForskjøvedeVilkårResultater))
}

fun Map<Vilkår, List<Periode<VilkårResultat>>>.tilLovverkTidslinje(barn: Person): Tidslinje<Lovverk> =
    this.values
        .map { perioder ->
            perioder
                .map { periode ->
                    Periode(
                        fom = periode.fom,
                        tom = periode.tom,
                        verdi =
                            LovverkUtleder.utledLovverkForBarn(
                                barn.fødselsdato,
                                skalBestemmeLovverkBasertPåFødselsdato = false,
                            ),
                    )
                }.tilTidslinje()
                .slåSammenLikePerioder()
        }.kombiner {
            it.toSet().single()
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
