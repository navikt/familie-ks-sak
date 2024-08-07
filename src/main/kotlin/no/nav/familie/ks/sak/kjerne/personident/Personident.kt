package no.nav.familie.ks.sak.kjerne.personident

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Pattern
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.time.LocalDateTime
import java.util.Objects

@Entity(name = "Personident")
@Table(name = "PERSONIDENT")
data class Personident(
    @Id
    @Column(name = "foedselsnummer", nullable = false)
    // Lovlige typer er fnr, dnr eller npid
    // Validator kommer til å først virke i Spring 3.0 grunnet at hibernate har tatt i bruk Jakarta.
    @Pattern(regexp = VALID_FØDSELSNUMMER)
    val fødselsnummer: String,
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,
    @Column(name = "aktiv", nullable = false)
    var aktiv: Boolean = true,
    @Column(name = "gjelder_til", columnDefinition = "DATE")
    var gjelderTil: LocalDateTime? = null,
) : BaseEntitet() {
    init {
        require(VALID.matcher(fødselsnummer).matches()) {
            "Ugyldig fødselsnummer, støtter kun 11 siffer."
        }
    }

    override fun toString(): String =
        """Personident(aktørId=${aktør.aktørId},
                        |aktiv=$aktiv
                        |gjelderTil=$gjelderTil)
        """.trimMargin()

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val entitet: Personident = other as Personident
        if (fødselsnummer != other.fødselsnummer) return false
        return Objects.equals(hashCode(), entitet.hashCode())
    }

    override fun hashCode(): Int = Objects.hash(fødselsnummer)

    companion object {
        private const val VALID_FØDSELSNUMMER = "^\\d{11}$"
        private val VALID =
            java.util.regex.Pattern
                .compile(VALID_FØDSELSNUMMER)
    }
}
