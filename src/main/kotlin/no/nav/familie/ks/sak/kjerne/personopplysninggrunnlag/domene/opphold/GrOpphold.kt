package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.opphold

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "GrOpphold")
@Table(name = "PO_OPPHOLD")
data class GrOpphold(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_opphold_seq_generator")
    @SequenceGenerator(
        name = "po_opphold_seq_generator",
        sequenceName = "po_opphold_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Embedded
    val gyldigPeriode: DatoIntervallEntitet? = null,

    @Column(name = "type", nullable = false)
    val type: OPPHOLDSTILLATELSE,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrOpphold

        if (gyldigPeriode != other.gyldigPeriode) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * gyldigPeriode.hashCode() + type.hashCode()
    }

    companion object {

        fun fraOpphold(opphold: Opphold, person: Person) = GrOpphold(
            gyldigPeriode = DatoIntervallEntitet(fom = opphold.oppholdFra, tom = opphold.oppholdTil),
            type = opphold.type,
            person = person
        )
    }
}
