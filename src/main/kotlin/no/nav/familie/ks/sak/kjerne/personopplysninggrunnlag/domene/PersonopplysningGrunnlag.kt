package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
data class PersonopplysningGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR")
    @SequenceGenerator(
        name = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR",
        sequenceName = "GR_PERSONOPPLYSNINGER_SEQ",
        allocationSize = 50
    )
    val id: Long = 0,

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personopplysningGrunnlag",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH]
    )
    val personer: MutableSet<Person> = mutableSetOf(),

    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true

) : BaseEntitet() {

    override fun toString(): String {
        return "PersonopplysningGrunnlagEntitet{id=$id,personer=$personer,aktiv=$aktiv}"
    }
}
