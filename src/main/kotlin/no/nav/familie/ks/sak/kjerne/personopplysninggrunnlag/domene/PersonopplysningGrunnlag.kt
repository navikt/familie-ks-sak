package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.exception.Feil

@Entity
@Table(name = "GR_PERSONOPPLYSNINGER")
data class PersonopplysningGrunnlag(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR")
    @SequenceGenerator(
        name = "GR_PERSONOPPLYSNINGER_SEQ_GENERATOR",
        sequenceName = "GR_PERSONOPPLYSNINGER_SEQ",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "personopplysningGrunnlag",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH],
    )
    val personer: MutableSet<Person> = mutableSetOf(),
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,
) : BaseEntitet() {
    override fun toString(): String = "PersonopplysningGrunnlagEntitet{id=$id,personer=$personer,aktiv=$aktiv}"

    val barna: List<Person> get() = personer.filter { it.type == PersonType.BARN }

    val søker: Person
        get() =
            personer.singleOrNull { it.type == PersonType.SØKER }
                ?: throw Feil("Persongrunnlag mangler søker eller det finnes flere personer i grunnlaget med type=SØKER")
}
