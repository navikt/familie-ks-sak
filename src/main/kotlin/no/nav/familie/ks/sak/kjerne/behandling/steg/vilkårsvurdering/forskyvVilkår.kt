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
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvBarnehageplassVilkår
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag

fun Collection<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeMap(personopplysningGrunnlag: PersonopplysningGrunnlag): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    personopplysningGrunnlag.personer.associate { person ->
        Pair(
            person.aktør,
            this.tilFørskjøvetVilkårResultatTidslinjeForPerson(person)
        )
    }

/***
 * Forskyver vilkårene til periodene de er oppfylt for.
 * Tar kun med periodene der alle vilkår er oppfylt.
 */
fun Collection<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeForPerson(
    person: Person
): Tidslinje<List<VilkårResultat>> {
    val personResultat = this.find { it.aktør == person.aktør }

    val vilkårResultaterForAktør = personResultat?.vilkårResultater ?: emptyList()

    val vilkårResultaterForAktørMap = vilkårResultaterForAktør
        .groupByTo(mutableMapOf()) { it.vilkårType }
        .mapValues { if (it.key == Vilkår.BOR_MED_SØKER) it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() else it.value }

    val forskjøvedeVilkårResultater = vilkårResultaterForAktørMap.map { (vilkårType, vilkårResultater) ->
        forskyvVilkårResultater(vilkårType, vilkårResultater).tilTidslinje()
    }

    return forskjøvedeVilkårResultater
        .kombiner { alleVilkårOppfyltEllerNull(it, person.type) }
        .tilPerioderIkkeNull()
        .tilTidslinje()
}

data class VilkårResultaterMedInformasjonOmNestePeriode(
    val vilkårResultat: VilkårResultat,
    val slutterDagenFørNeste: Boolean
)

fun forskyvVilkårResultater(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>
): List<Periode<VilkårResultat>> = when (vilkårType) {
    Vilkår.BARNEHAGEPLASS -> vilkårResultater.forskyvBarnehageplassVilkår()

    else -> tilVilkårResultaterMedInformasjonOmNestePeriode(vilkårResultater)
        .map {
            val forskjøvetTom = if (it.slutterDagenFørNeste) {
                it.vilkårResultat.periodeTom?.plusDays(1)?.sisteDagIMåned()
            } else it.vilkårResultat.periodeTom?.minusMonths(1)?.sisteDagIMåned()

            Periode(
                verdi = it.vilkårResultat,
                fom = it.vilkårResultat.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
                tom = forskjøvetTom
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
}

private fun tilVilkårResultaterMedInformasjonOmNestePeriode(vilkårResultater: List<VilkårResultat>) =
    vilkårResultater.zipWithNext { denne, neste ->
        VilkårResultaterMedInformasjonOmNestePeriode(
            denne,
            denne.periodeTom?.erDagenFør(
                neste.periodeFom
            ) ?: false
        )
    } + VilkårResultaterMedInformasjonOmNestePeriode(vilkårResultater.last(), false)

private fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
    if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

private fun alleVilkårOppfyltEllerNull(
    vilkårResultater: Iterable<VilkårResultat?>,
    personType: PersonType
): List<VilkårResultat>? {
    val vilkårForPerson = Vilkår.hentVilkårFor(personType)

    return if (erAlleVilkårForPersonOppfylt(vilkårForPerson, vilkårResultater)) {
        vilkårResultater.filterNotNull()
    } else null
}

private fun erAlleVilkårForPersonOppfylt(
    vilkårForPerson: Set<Vilkår>, vilkårResultater: Iterable<VilkårResultat?>
) = vilkårForPerson.all { vilkår ->
    vilkårResultater.any {
        val erOppfyltEllerIkkeAktuelt = it?.resultat == Resultat.OPPFYLT || it?.resultat == Resultat.IKKE_AKTUELT

        erOppfyltEllerIkkeAktuelt && it?.vilkårType == vilkår
    }
}
