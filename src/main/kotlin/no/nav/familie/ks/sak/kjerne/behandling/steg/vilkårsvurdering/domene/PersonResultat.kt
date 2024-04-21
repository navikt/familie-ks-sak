package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat.Companion.VilkårResultatComparator
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType

@Entity(name = "PersonResultat")
@Table(name = "person_resultat")
class PersonResultat(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_resultat_seq_generator")
    @SequenceGenerator(
        name = "person_resultat_seq_generator",
        sequenceName = "person_resultat_seq",
        allocationSize = 50,
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
        orphanRemoval = true,
    )
    val vilkårResultater: MutableSet<VilkårResultat> = sortedSetOf(VilkårResultatComparator),
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personResultat",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    val andreVurderinger: MutableSet<AnnenVurdering> = mutableSetOf(),
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
                type = annenVurderingType,
            ),
        )
    }

    fun harEksplisittAvslag() = vilkårResultater.any { it.erEksplisittAvslagPåSøknad == true }
}
