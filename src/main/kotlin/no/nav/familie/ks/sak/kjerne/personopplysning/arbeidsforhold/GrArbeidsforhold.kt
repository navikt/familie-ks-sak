package no.nav.familie.ks.sak.kjerne.personopplysning.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.kjerne.personopplysning.domene.Person
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

@Entity(name = "GrArbeidsforhold")
@Table(name = "PO_ARBEIDSFORHOLD")
data class GrArbeidsforhold(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_arbeidsforhold_seq_generator")
    @SequenceGenerator(
        name = "po_arbeidsforhold_seq_generator",
        sequenceName = "po_arbeidsforhold_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Embedded
    val periode: DatoIntervallEntitet? = null,

    @Column(name = "arbeidsgiver_id")
    val arbeidsgiverId: String?,

    @Column(name = "arbeidsgiver_type")
    val arbeidsgiverType: String?,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person
) : BaseEntitet()
