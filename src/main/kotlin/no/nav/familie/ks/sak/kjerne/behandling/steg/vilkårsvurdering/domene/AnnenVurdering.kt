package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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
) : BaseEntitet() {

    fun kopierMedParent(nyPersonResultat: PersonResultat? = null): AnnenVurdering {
        return AnnenVurdering(
            personResultat = nyPersonResultat ?: personResultat,
            type = type,
            resultat = resultat,
            begrunnelse = begrunnelse
        )
    }
}

enum class AnnenVurderingType {
    OPPLYSNINGSPLIKT
}
