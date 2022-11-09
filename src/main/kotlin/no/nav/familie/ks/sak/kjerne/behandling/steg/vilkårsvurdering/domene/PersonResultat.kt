package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombiner
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ks.sak.kjerne.beregning.domene.hentProsentForAntallTimer
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "PersonResultat")
@Table(name = "person_resultat")
class PersonResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_resultat_seq_generator")
    @SequenceGenerator(
        name = "person_resultat_seq_generator",
        sequenceName = "person_resultat_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_vilkaarsvurdering_id", nullable = false, updatable = false)
    var vilkårsvurdering: Vilkårsvurdering,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personResultat",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val vilkårResultater: MutableSet<VilkårResultat> = sortedSetOf(VilkårResultatComparator),

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personResultat",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    val andreVurderinger: MutableSet<AnnenVurdering> = mutableSetOf()
) : BaseEntitet() {

    fun setSortedVilkårResultater(nyeVilkårResultater: Set<VilkårResultat>) {
        vilkårResultater.clear()
        vilkårResultater.addAll(nyeVilkårResultater.toSortedSet(VilkårResultatComparator))
    }

    fun erSøkersResultater() = vilkårResultater.all { it.vilkårType.parterDetteGjelderFor.contains(PersonType.SØKER) }

    fun leggTilBlankAnnenVurdering(annenVurderingType: AnnenVurderingType) {
        this.andreVurderinger.add(
            AnnenVurdering(
                personResultat = this,
                type = annenVurderingType
            )
        )
    }

    fun harEksplisittAvslag() = vilkårResultater.any { it.erEksplisittAvslagPåSøknad == true }
}

/***
 * Gir en tidslinje for hver person.
 * Tar kun med periodene der alle vilkår er oppfylt.
 * Forskyver vilkårene til periodene de gjelder for.
 */
fun Collection<PersonResultat>.tilFørskjøvetVilkårResultatTidslinjeMap(personopplysningGrunnlag: PersonopplysningGrunnlag): Map<Aktør, Tidslinje<List<VilkårResultat>>> =
    associate { personResultat ->
        val personType = personopplysningGrunnlag.personer.single { it.aktør == personResultat.aktør }.type

        val vilkårResultaterForAktørMap = personResultat.vilkårResultater
            .groupByTo(mutableMapOf()) { it.vilkårType }
            .mapValues { if (it.key != Vilkår.BOR_MED_SØKER) it.value else it.value.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat() }

        val forskjøvedeVilkårResultater = vilkårResultaterForAktørMap.map { (vilkårType, vilkårResultater) ->
            forskyvVilkårResultater(vilkårType, vilkårResultater).tilTidslinje()
        }

        val vilkårResultaterKombinert = forskjøvedeVilkårResultater
            .kombiner { alleVilkårOppfyltEllerNull(it, personType) }
            .tilPerioderIkkeNull()
            .tilTidslinje()

        Pair(
            personResultat.aktør,
            vilkårResultaterKombinert
        )
    }

private fun forskyvVilkårResultater(
    vilkårType: Vilkår,
    vilkårResultater: List<VilkårResultat>
) = when (vilkårType) {
    Vilkår.BARNEHAGEPLASS -> vilkårResultater.forskyvBarnehageplassVilkår()

    Vilkår.MELLOM_1_OG_2_ELLER_ADOPTERT -> vilkårResultater.map {
        Periode(
            verdi = it,
            fom = it.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
            tom = it.periodeTom?.minusMonths(1)?.sisteDagIMåned()
        )
    }

    else -> vilkårResultater.map {
        Periode(
            verdi = it,
            fom = it.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned(),
            tom = it.periodeTom?.plusMonths(1)?.sisteDagIMåned()
        )
    }
}

enum class Graderingsforskjell {
    Øking,
    Reduksjon,
    Lik
}

data class BarnehageplassVilkårMedDifferanserMellomPerioder(
    val vilkår: VilkårResultat,
    val graderingsforskjellFraForrigePeriode: Graderingsforskjell,
    val graderingsforskjellTilNestePeriode: Graderingsforskjell
)

fun List<VilkårResultat>.forskyvBarnehageplassVilkår(): List<Periode<VilkårResultat>> {
    return this
        .tilBarnehageplassVilkårMedDifferanserMellomPerioder()
        .map {
            val fom =
                if (it.graderingsforskjellFraForrigePeriode == Graderingsforskjell.Reduksjon) {
                    it.vilkår.periodeFom?.førsteDagIInneværendeMåned()
                } else it.vilkår.periodeFom?.plusMonths(1)?.førsteDagIInneværendeMåned()
            val tom =
                if (it.graderingsforskjellTilNestePeriode == Graderingsforskjell.Reduksjon) {
                    it.vilkår.periodeTom?.plusDays(1)?.minusMonths(1)?.sisteDagIMåned()
                } else it.vilkår.periodeTom?.plusDays(1)?.sisteDagIMåned()

            Periode(
                verdi = it.vilkår,
                fom = fom,
                tom = tom
            )
        }.filter { (it.fom ?: TIDENES_MORGEN).isBefore(it.tom ?: TIDENES_ENDE) }
        .map { listOf(it).tilTidslinje() }
        .kombiner { vilkårResultater -> vilkårResultater.maxByOrNull { it.antallTimer ?: BigDecimal.ZERO } }.tilPerioderIkkeNull()
}

private fun List<VilkårResultat>.tilBarnehageplassVilkårMedDifferanserMellomPerioder(): List<BarnehageplassVilkårMedDifferanserMellomPerioder> {
    return this
        .filter { it.resultat == Resultat.OPPFYLT }
        .sortedBy { it.periodeFom ?: TIDENES_MORGEN }
        .fold(emptyList()) { acc: List<BarnehageplassVilkårMedDifferanserMellomPerioder>, vilkårResultat: VilkårResultat ->
            if (acc.isEmpty()) {
                val gradering = hentProsentForAntallTimer(vilkårResultat.antallTimer)
                listOf(
                    BarnehageplassVilkårMedDifferanserMellomPerioder(
                        vilkår = vilkårResultat,
                        graderingsforskjellFraForrigePeriode = if (gradering > BigDecimal.ZERO) Graderingsforskjell.Øking else Graderingsforskjell.Lik,
                        graderingsforskjellTilNestePeriode = Graderingsforskjell.Reduksjon
                    )
                )
            } else {
                val forrige = acc.last()

                val graderingForrigePeriode = hentProsentForAntallTimer(forrige.vilkår.antallTimer)
                val graderingNestePeriode = hentProsentForAntallTimer(vilkårResultat.antallTimer)
                val graderingsforskjell = when {
                    graderingForrigePeriode > graderingNestePeriode -> Graderingsforskjell.Reduksjon
                    graderingForrigePeriode < graderingNestePeriode -> Graderingsforskjell.Øking
                    else -> Graderingsforskjell.Lik
                }

                acc.dropLast(1) + forrige.copy(graderingsforskjellTilNestePeriode = graderingsforskjell) + BarnehageplassVilkårMedDifferanserMellomPerioder(
                    vilkårResultat,
                    graderingsforskjell,
                    Graderingsforskjell.Reduksjon
                )
            }
        }
}

private fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
    if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

private fun Map<Vilkår, List<VilkårResultat>>.tilVilkårResultatTidslinjer() =
    this.map { (_, vilkårResultater) ->
        vilkårResultater.map { Periode(it, it.periodeFom, it.periodeTom) }.tilTidslinje()
    }

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
    vilkårForPerson: Set<Vilkår>,
    vilkårResultater: Iterable<VilkårResultat?>
) =
    vilkårForPerson.all { vilkår -> vilkårResultater.any { it?.resultat == Resultat.OPPFYLT && it.vilkårType == vilkår } }
