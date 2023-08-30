package no.nav.familie.ks.sak.kjerne.personident

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Pattern
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import java.util.Objects

/**
 * Id som genereres fra NAV Aktør Register. Denne iden benyttes til interne forhold i Nav og vil ikke endres f.eks. dersom bruker
 * går fra DNR til FNR i Folkeregisteret. Tilsvarende vil den kunne referere personer som har ident fra et utenlandsk system.
 */
@Entity(name = "Aktør")
@Table(name = "AKTOER")
data class Aktør(
    // Er ikke kalt id ettersom den refererer til en ekstern id.
    @Id
    @Column(name = "aktoer_id", updatable = false, length = 50)
    // Validator kommer til å først virke i Spring 3.0 grunnet at hibernate har tatt i bruk Jakarta.
    @Pattern(regexp = VALID_REGEXP)
    val aktørId: String,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "aktør",
        cascade = [CascadeType.ALL]
    )
    val personidenter: MutableSet<Personident> = mutableSetOf()
) : BaseEntitet() {

    init {
        require(VALID.matcher(aktørId).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            "Ugyldig aktør, støtter kun 13 siffer."
        }
    }

    override fun toString(): String {
        return """aktørId=$aktørId""".trimMargin()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherAktør: Aktør = other as Aktør
        return aktørId == otherAktør.aktørId
    }

    override fun hashCode(): Int {
        return Objects.hash(aktørId)
    }

    fun aktivFødselsnummer() = personidenter.single { it.aktiv }.fødselsnummer

    fun harIdent(fødselsnummer: String) = personidenter.any { it.fødselsnummer == fødselsnummer }

    companion object {
        private const val VALID_REGEXP = "^\\d{13}$"
        private val VALID = java.util.regex.Pattern.compile(VALID_REGEXP)
    }
}
