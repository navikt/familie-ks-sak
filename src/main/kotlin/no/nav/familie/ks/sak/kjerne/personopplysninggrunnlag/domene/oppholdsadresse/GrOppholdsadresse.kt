package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrMatrikkeladresseOppholdsadresse.Companion.fraMatrikkeladresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrUtenlandskAdresseOppholdsadresse.Companion.fraUtenlandskAdresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse.GrVegadresseOppholdsadresse.Companion.fraVegadresse

@Entity(name = "GrOppholdsadresse")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Table(name = "PO_OPPHOLDSADRESSE")
abstract class GrOppholdsadresse(
    // Alle attributter må være open ellers kastes feil ved oppstart.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_oppholdsadresse_seq_generator")
    @SequenceGenerator(
        name = "po_oppholdsadresse_seq_generator",
        sequenceName = "po_oppholdsadresse_seq",
        allocationSize = 50,
    )
    open val id: Long = 0,
    @Embedded
    open var periode: DatoIntervallEntitet? = null,
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    open var person: Person? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "opphold_annet_sted")
    open var oppholdAnnetSted: OppholdAnnetSted? = null,
) : BaseEntitet() {
    abstract fun toSecureString(): String

    abstract fun tilFrontendString(): String

    abstract fun tilAdresse(): Adresse

    abstract fun erPåSvalbard(): Boolean

    companion object {
        fun fraOppholdsadresse(
            oppholdsadresse: Oppholdsadresse,
            person: Person,
            poststed: String? = null,
        ): GrOppholdsadresse =
            (
                oppholdsadresse.vegadresse?.let { fraVegadresse(it, poststed) }
                    ?: oppholdsadresse.matrikkeladresse?.let { fraMatrikkeladresse(it, poststed) }
                    ?: oppholdsadresse.utenlandskAdresse?.let { fraUtenlandskAdresse(it) }
                    ?: GrUkjentAdresseOppholdsadresse()
            ).also {
                it.person = person
                it.periode = DatoIntervallEntitet(oppholdsadresse.gyldigFraOgMed, oppholdsadresse.gyldigTilOgMed)
                it.oppholdAnnetSted = OppholdAnnetSted.parse(oppholdsadresse.oppholdAnnetSted)
            }
    }
}
