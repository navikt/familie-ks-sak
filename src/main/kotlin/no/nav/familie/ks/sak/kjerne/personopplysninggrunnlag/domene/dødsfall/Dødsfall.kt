package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlKontaktinformasjonForDødsboAdresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.LocalDate

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
    val dødsfallPoststed: String?,
) : BaseEntitet() {
    fun hentAdresseToString(): String {
        return """$dødsfallAdresse, $dødsfallPostnummer $dødsfallPoststed"""
    }

    companion object {
        fun lagDødsfall(
            person: Person,
            pdlDødsfallDato: String?,
            pdlDødsfallAdresse: PdlKontaktinformasjonForDødsboAdresse?,
        ): Dødsfall? {
            if (pdlDødsfallDato.isNullOrEmpty()) return null
            return Dødsfall(
                person = person,
                dødsfallDato = LocalDate.parse(pdlDødsfallDato),
                dødsfallAdresse = pdlDødsfallAdresse?.adresselinje1,
                dødsfallPostnummer = pdlDødsfallAdresse?.postnummer,
                dødsfallPoststed = pdlDødsfallAdresse?.poststedsnavn,
            )
        }
    }
}
