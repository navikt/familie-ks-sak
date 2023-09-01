package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.erDagenFør
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

fun Collection<PersonResultat>.tilFørskjøvetOppfylteVilkårResultatTidslinjeMap(personopplysningGrunnlag: PersonopplysningGrunnlag): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilFørskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(person),
        )
    }

fun Collection<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeMap(personopplysningGrunnlag: PersonopplysningGrunnlag): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilFørskjøvetVilkårResultatTidslinjeForPerson(person),
        )
    }

fun Collection<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeForPerson(
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

    val vilkårResultaterForAktørMap = vilkårResultaterForAktør
        .groupByTo(mutableMapOf()) { it.vilkårType }
        .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

    val forskjøvedeVilkårResultater = vilkårResultaterForAktørMap.map { (vilkårType, vilkårResultater) ->
        forskyvVilkårResultater(vilkårType, vilkårResultater).tilTidslinje()
    }
    return forskjøvedeVilkårResultater
}

/***
 * Forskyver vilkårene til periodene de er oppfylt for.
 * Tar kun med periodene der alle vilkår er oppfylt.
 */
fun Collection<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeDerVilkårErOppfyltForPerson(
    person: Person,
): Tidslinje<List<VilkårResultat>> {
    val forskjøvedeVilkårResultater = forskyvVilkårResultaterForPerson(person)

    return forskjøvedeVilkårResultater
        .kombiner { alleVilkårOppfyltEllerNull(it, person.type) }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

data class VilkårResultaterMedInformasjonOmNestePeriode(
    val vilkårResultat: VilkårResultat,
    val slutterDagenFørNeste: Boolean,
    val slutterPåSisteDagIMåneden: Boolean,
)

fun forskyvVilkårResultater(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>,
): List<Periode<VilkårResultat>> = when (vilkårType) {
    Vilkår.BARNEHAGEPLASS -> vilkårResultater.forskyvBarnehageplassVilkår()

    else -> tilVilkårResultaterMedInformasjonOmNestePeriode(
        vilkårResultater.filter { it.erOppfylt() || it.erIkkeAktuelt() }.sortedBy { it.periodeFom },
    )
        .map {
            val forskjøvetTom = when {
                it.slutterDagenFørNeste -> {
                    it.vilkårResultat.periodeTom?.plusDays(1)?.sisteDagIMåned()
                }

                it.slutterPåSisteDagIMåneden -> { // Hvis perioden slutter siste dag i måned, får man kontantstøtte i denne måneden
                    it.vilkårResultat.periodeTom?.sisteDagIMåned()
                }

                else -> it.vilkårResultat.periodeTom?.minusMonths(1)?.sisteDagIMåned()
            }

            Periode(
                verdi = it.vilkårResultat,
                fom = it.vilkårResultat.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
                tom = forskjøvetTom,
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
}

private fun tilVilkårResultaterMedInformasjonOmNestePeriode(vilkårResultater: List<VilkårResultat>): List<VilkårResultaterMedInformasjonOmNestePeriode> {
    if (vilkårResultater.isEmpty()) return emptyList()

    return vilkårResultater.zipWithNext { denne, neste ->
        VilkårResultaterMedInformasjonOmNestePeriode(
            vilkårResultat = denne,
            slutterDagenFørNeste = denne.periodeTom?.erDagenFør(neste.periodeFom) ?: false,
            slutterPåSisteDagIMåneden = denne.periodeTom != null &&
                denne.periodeTom == denne.periodeTom?.tilYearMonth()?.atEndOfMonth(),
        )
    } + VilkårResultaterMedInformasjonOmNestePeriode(vilkårResultater.last(), false, false)
}

fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
    if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

private fun alleVilkårOppfyltEllerNull(
    vilkårResultater: Iterable<VilkårResultat?>,
    personType: PersonType,
): List<VilkårResultat>? {
    val vilkårForPerson = Vilkår.hentVilkårFor(personType)

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
