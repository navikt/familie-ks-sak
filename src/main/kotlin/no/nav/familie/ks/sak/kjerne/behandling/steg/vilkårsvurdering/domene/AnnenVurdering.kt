package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet

@Entity(name = "AnnenVurdering")
@Table(name = "annen_vurdering")
class AnnenVurdering(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annen_vurdering_seq_generator")
    @SequenceGenerator(
        name = "annen_vurdering_seq_generator",
        sequenceName = "annen_vurdering_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "fk_person_resultat_id")
    var personResultat: PersonResultat,

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat")
    var resultat: Resultat = Resultat.IKKE_VURDERT,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    var type: AnnenVurderingType,

    @Column(name = "begrunnelse")
    var begrunnelse: String? = null
) : BaseEntitet()

enum class AnnenVurderingType {
    OPPLYSNINGSPLIKT
}
