package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.ks.sak.common.util.nullableTilString
import no.nav.familie.ks.sak.common.util.storForbokstav
import java.util.Objects

@Entity(name = "GrVegadresseBostedsadresse")
@DiscriminatorValue("Vegadresse")
data class GrVegadresseBostedsadresse(
    @Column(name = "matrikkel_id")
    val matrikkelId: Long?,
    @Column(name = "husnummer")
    val husnummer: String?,
    @Column(name = "husbokstav")
    val husbokstav: String?,
    @Column(name = "bruksenhetsnummer")
    val bruksenhetsnummer: String?,
    @Column(name = "adressenavn")
    val adressenavn: String?,
    @Column(name = "kommunenummer")
    val kommunenummer: String?,
    @Column(name = "tilleggsnavn")
    val tilleggsnavn: String?,
    @Column(name = "postnummer")
    val postnummer: String?,
) : GrBostedsadresse() {
    override fun toSecureString(): String =
        """GrVegadresseBostedsadresse(husnummer=$husnummer,husbokstav=$husbokstav,matrikkelId=$matrikkelId,
                |bruksenhetsnummer=$bruksenhetsnummer,adressenavn=$adressenavn,kommunenummer=$kommunenummer,
                |tilleggsnavn=$tilleggsnavn,postnummer=$postnummer
        """.trimMargin()

    override fun toString(): String = "GrVegadresseBostedsadresse(detaljer skjult)"

    override fun tilFrontendString() =
        """${adressenavn.nullableTilString().storForbokstav()} 
    |${husnummer.nullableTilString()}${husbokstav.nullableTilString()}${postnummer.let { ", $it" }} """.trimMargin()

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherVegadresse = other as GrVegadresseBostedsadresse

        return this === other ||
            (
                (matrikkelId != null && matrikkelId == otherVegadresse.matrikkelId) ||
                    (
                        (matrikkelId == null && otherVegadresse.matrikkelId == null) &&
                            postnummer != null &&
                            !(adressenavn == null && husnummer == null && husbokstav == null) &&
                            (adressenavn == otherVegadresse.adressenavn) &&
                            (husnummer == otherVegadresse.husnummer) &&
                            (husbokstav == otherVegadresse.husbokstav) &&
                            (postnummer == otherVegadresse.postnummer)
                    )
            )
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    companion object {
        fun fraVegadresse(vegadresse: Vegadresse): GrVegadresseBostedsadresse =
            GrVegadresseBostedsadresse(
                matrikkelId = vegadresse.matrikkelId,
                husnummer = vegadresse.husnummer,
                husbokstav = vegadresse.husbokstav,
                bruksenhetsnummer = vegadresse.bruksenhetsnummer,
                adressenavn = vegadresse.adressenavn,
                kommunenummer = vegadresse.kommunenummer,
                tilleggsnavn = vegadresse.tilleggsnavn,
                postnummer = vegadresse.postnummer,
            )
    }
}
