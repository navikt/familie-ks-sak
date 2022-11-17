package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "Vilkårsvurdering")
@Table(name = "vilkaarsvurdering")
data class Vilkårsvurdering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkaarsvurdering_seq_generator")
    @SequenceGenerator(
        name = "vilkaarsvurdering_seq_generator",
        sequenceName = "vilkaarsvurdering_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "vilkårsvurdering",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH]
    )
    var personResultater: Set<PersonResultat> = setOf(),

    @Column(name = "ytelse_personer", columnDefinition = "text")
    var ytelsePersoner: String? = null
) : BaseEntitet() {

    fun hentPersonResultaterTilAktør(aktørId: String): List<VilkårResultat> =
        personResultater.find { it.aktør.aktørId == aktørId }?.vilkårResultater?.toList()
            ?: throw IllegalStateException("Fant ikke personresultat for $aktørId")

    fun finnOpplysningspliktVilkår(): AnnenVurdering? {
        return personResultater.single { it.erSøkersResultater() }
            .andreVurderinger.singleOrNull { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }
    }
}
