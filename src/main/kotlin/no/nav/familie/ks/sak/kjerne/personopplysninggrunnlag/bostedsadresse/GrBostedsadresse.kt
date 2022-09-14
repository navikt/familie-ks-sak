package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import javax.persistence.DiscriminatorColumn
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "GrBostedsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_BOSTEDSADRESSE")
abstract class GrBostedsadresse(
    // Alle attributter må være open ellers kastes feil ved oppsrart.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresse_seq_generator")
    @SequenceGenerator(
        name = "po_bostedsadresse_seq_generator",
        sequenceName = "po_bostedsadresse_seq",
        allocationSize = 50
    )
    open val id: Long = 0,

    @Embedded
    open var periode: DatoIntervallEntitet? = null,

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    open var person: Person? = null
) : BaseEntitet() {

    abstract fun toSecureString(): String

    abstract fun tilFrontendString(): String
}
