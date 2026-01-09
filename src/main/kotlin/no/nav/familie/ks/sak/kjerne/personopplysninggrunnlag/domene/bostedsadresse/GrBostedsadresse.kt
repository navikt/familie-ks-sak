package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresse

@Entity(name = "GrBostedsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_BOSTEDSADRESSE")
abstract class GrBostedsadresse(
    // Alle attributter må være open ellers kastes feil ved oppstart.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_bostedsadresse_seq_generator")
    @SequenceGenerator(
        name = "po_bostedsadresse_seq_generator",
        sequenceName = "po_bostedsadresse_seq",
        allocationSize = 50,
    )
    open val id: Long = 0,
    @Embedded
    open var periode: DatoIntervallEntitet? = null,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    open var person: Person? = null,
) : BaseEntitet() {
    abstract fun toSecureString(): String

    abstract fun tilFrontendString(): String

    abstract fun tilAdresse(): Adresse

    companion object {
        fun fraBostedsadresse(
            bostedsadresse: Bostedsadresse,
            person: Person,
            poststed: String? = null,
        ): GrBostedsadresse {
            val mappetAdresse =
                bostedsadresse.vegadresse?.let { GrVegadresseBostedsadresse.fraVegadresse(it, poststed) }
                    ?: bostedsadresse.matrikkeladresse?.let { GrMatrikkeladresseBostedsadresse.fraMatrikkeladresse(it, poststed) }
                    ?: bostedsadresse.ukjentBosted?.let { GrUkjentBostedBostedsadresse.fraUkjentBosted(it) }
                    ?: throw Feil("Vegadresse, matrikkeladresse og ukjent bosted har verdi null ved mapping fra bostedadresse")

            return mappetAdresse.also {
                it.person = person
                it.periode = DatoIntervallEntitet(bostedsadresse.angittFlyttedato, bostedsadresse.gyldigTilOgMed)
            }
        }
    }
}
