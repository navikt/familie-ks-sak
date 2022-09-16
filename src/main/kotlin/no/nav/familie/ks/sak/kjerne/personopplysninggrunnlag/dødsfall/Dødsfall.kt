package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.dødsfall

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "Dødsfall")
@Table(name = "po_doedsfall")
data class Dødsfall(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_doedsfall_seq_generator")
    @SequenceGenerator(name = "po_doedsfall_seq_generator", sequenceName = "po_doedsfall_seq", allocationSize = 50)
    val id: Long = 0,

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "fk_po_person_id", referencedColumnName = "id", nullable = false)
    val person: Person,

    @Column(name = "doedsfall_dato", nullable = false)
    val dødsfallDato: LocalDate,

    @Column(name = "doedsfall_adresse", nullable = true)
    val dødsfallAdresse: String?,

    @Column(name = "doedsfall_postnummer", nullable = true)
    val dødsfallPostnummer: String?,

    @Column(name = "doedsfall_poststed", nullable = true)
    val dødsfallPoststed: String?
) : BaseEntitet() {
    fun hentAdresseToString(): String {
        return """$dødsfallAdresse, $dødsfallPostnummer $dødsfallPoststed"""
    }
}
