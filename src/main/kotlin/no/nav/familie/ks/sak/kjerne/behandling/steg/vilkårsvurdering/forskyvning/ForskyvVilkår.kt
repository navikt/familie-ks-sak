package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFebruar2025.ForskyvVilkårLovendringFebruar2025
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
import no.nav.familie.tidslinje.tomTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull

fun Collection<PersonResultat>.tilForskjøvetOppfylteVilkårResultatTidslinjeMap(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    this
        .forskyvVilkårResultater(
            personopplysningGrunnlag = personopplysningGrunnlag,
            adopsjonerIBehandling = adopsjonerIBehandling,
        ).mapValues { entry ->
            val person = personopplysningGrunnlag.personer.single { it.aktør == entry.key }
            entry.value.tilOppfylteVilkårTidslinje(person.type)
        }

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeMap(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    this
        .forskyvVilkårResultater(
            personopplysningGrunnlag = personopplysningGrunnlag,
            adopsjonerIBehandling = adopsjonerIBehandling,
        ).mapValues {
            it.value.tilVilkårResultaterTidslinje()
        }

fun Collection<PersonResultat>.tilForskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
    person: Person,
    lovverk: Lovverk = Lovverk.FØR_LOVENDRING_2025,
): Tidslinje<List<VilkårResultat>> =
    this
        .single { it.aktør == person.aktør }
        .forskyvVilkårResultater(lovverk = lovverk)
        .tilOppfylteVilkårTidslinje(person.type)

private fun Map<Vilkår, List<Periode<VilkårResultat>>>.tilOppfylteVilkårTidslinje(personType: PersonType) =
    this
        .map { it.value.tilTidslinje() }
        .kombiner { alleVilkårOppfyltEllerNull(it, personType) }
        .tilPerioderIkkeNull()
        .tilTidslinje()

private fun Map<Vilkår, List<Periode<VilkårResultat>>>.tilVilkårResultaterTidslinje() =
    this
        .map { it.value.tilTidslinje() }
        .kombiner { it.toList() }
        .tilPerioderIkkeNull()
        .tilTidslinje()

fun Collection<PersonResultat>.forskyvVilkårResultater(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): Map<Aktør, Map<Vilkår, List<Periode<VilkårResultat>>>> {
    // Forskyver barnas vilkår basert på barnets lovverk
    val barnasForskjøvedeVilkårResultater = this.forskyvBarnasVilkårResultater(personopplysningGrunnlag = personopplysningGrunnlag, adopsjonerIBehandling = adopsjonerIBehandling)

    // Lager lovverk-tidslinje basert på barnas forskjøvede VilkårResultater
    val lovverkTidslinje =
        LovverkTidslinjeGenerator.generer(
            barnasForskjøvedeVilkårResultater = barnasForskjøvedeVilkårResultater,
            personopplysningGrunnlag = personopplysningGrunnlag,
            adopsjonerIBehandling = adopsjonerIBehandling,
        )

    // Forskyver søker etter alle lovverk og kombinerer med lovverk-tidslinje
    val søkersForskjøvedeVilkårResultater = this.single { it.erSøkersResultater() }.forskyvSøkersVilkårResultater(lovverkTidslinje = lovverkTidslinje)

    return barnasForskjøvedeVilkårResultater.plus(Pair(this.single { it.erSøkersResultater() }.aktør, søkersForskjøvedeVilkårResultater))
}

fun PersonResultat.forskyvVilkårResultater(
    lovverk: Lovverk = Lovverk.FØR_LOVENDRING_2025,
): Map<Vilkår, List<Periode<VilkårResultat>>> =
    when (lovverk) {
        Lovverk.FØR_LOVENDRING_2025 -> ForskyvVilkårFørFebruar2025.forskyvVilkårResultater(this.vilkårResultater)
        Lovverk.LOVENDRING_FEBRUAR_2025 -> ForskyvVilkårLovendringFebruar2025.forskyvVilkårResultater(this.vilkårResultater)
    }

private fun Collection<PersonResultat>.forskyvBarnasVilkårResultater(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): Map<Aktør, Map<Vilkår, List<Periode<VilkårResultat>>>> =
    this.filter { !it.erSøkersResultater() }.associate { personResultat ->
        val lovverk =
            LovverkUtleder.utledLovverkForBarn(
                fødselsdato = personopplysningGrunnlag.barna.single { it.aktør == personResultat.aktør }.fødselsdato,
                adopsjonsdato = adopsjonerIBehandling.firstOrNull { it.aktør == personResultat.aktør }?.adopsjonsdato,
            )
        personResultat.aktør to personResultat.forskyvVilkårResultater(lovverk)
    }

private fun PersonResultat.forskyvSøkersVilkårResultater(lovverkTidslinje: Tidslinje<Lovverk>): Map<Vilkår, List<Periode<VilkårResultat>>> {
    if (!this.erSøkersResultater()) throw Feil("PersonResultat må være søkers resultat")
    // Forskyver alle VilkårResultater etter alle lovverk og kombinerer resulterende VilkårResultat-tidslinje med Lovverk-tidslinja.
    // Dette fører til at VilkårResultat-tidslinja per lovverk blir avgrenset til hvor lenge et bestemt lovverk er gjeldende.
    val lovverkIBehandling = lovverkTidslinje.tilPerioderIkkeNull().map { it.verdi }.toSet()
    val forskjøvedeVilkårResultaterEtterMuligeLovverkAvgrensetAvLovverkTidslinje =
        lovverkIBehandling
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
                            }
                    }
            }

    // Kombinerer alle tidslinjer per lovverk og vilkår til én tidslinje per vilkår
    return forskjøvedeVilkårResultaterEtterMuligeLovverkAvgrensetAvLovverkTidslinje
        .flatMap { it.keys }
        .associateWith { key ->
            forskjøvedeVilkårResultaterEtterMuligeLovverkAvgrensetAvLovverkTidslinje
                .map { it.getOrDefault(key, tomTidslinje()) }
                .reduce { kombinertTidslinje, tidslinjeForLovverk -> kombinertTidslinje.kombinerMed(tidslinjeForLovverk) { a, b -> a ?: b } }
                .tilPerioderIkkeNull()
        }
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
