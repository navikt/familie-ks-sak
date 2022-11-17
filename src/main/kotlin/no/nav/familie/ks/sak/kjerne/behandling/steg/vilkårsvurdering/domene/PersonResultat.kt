package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
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

private fun MutableList<VilkårResultat>.fjernAvslagUtenPeriodeHvisDetFinsAndreVilkårResultat(): List<VilkårResultat> =
    if (this.any { !it.erAvslagUtenPeriode() }) this.filterNot { it.erAvslagUtenPeriode() } else this

private fun Map<Vilkår, List<VilkårResultat>>.tilVilkårResultatTidslinjer() =
    this.map { (_, vilkårResultater) ->
        vilkårResultater.map { Periode(it, it.periodeFom, it.periodeTom) }.tilTidslinje()
    }

private fun alleVilkårOppfyltEllerNull(
    vilkårResultater: Iterable<VilkårResultat?>,
    vilkårForPerson: Set<Vilkår>
): List<VilkårResultat>? =
    if (erAlleVilkårForPersonOppfylt(vilkårForPerson, vilkårResultater)) {
        vilkårResultater.filterNotNull()
    } else null

private fun erAlleVilkårForPersonOppfylt(
    vilkårForPerson: Set<Vilkår>,
    vilkårResultater: Iterable<VilkårResultat?>
) =
    vilkårForPerson.all { vilkår -> vilkårResultater.any { it?.resultat == Resultat.OPPFYLT && it.vilkårType == vilkår } }
