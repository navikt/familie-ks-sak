package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.familie.kontrakter.felles.personopplysning.Matrikkeladresse
import java.util.Objects

@Entity(name = "GrMatrikkeladresseBostedsadresse")
@DiscriminatorValue("Matrikkeladresse")
data class GrMatrikkeladresseBostedsadresse(
    @Column(name = "matrikkel_id")
    val matrikkelId: Long?,
    @Column(name = "bruksenhetsnummer")
    val bruksenhetsnummer: String?,
    @Column(name = "tilleggsnavn")
    val tilleggsnavn: String?,
    @Column(name = "postnummer")
    val postnummer: String?,
    @Column(name = "poststed")
    val poststed: String?,
    @Column(name = "kommunenummer")
    val kommunenummer: String?,
) : GrBostedsadresse() {
    override fun toSecureString(): String =
        """GrMatrikkeladresseBostedsadresse(matrikkelId=$matrikkelId,bruksenhetsnummer=$bruksenhetsnummer,
            tilleggsnavn=$tilleggsnavn, postnummer=$postnummer,poststed=$poststed,kommunenummer=$kommunenummer
        """.trimMargin()

    override fun toString(): String = "GrMatrikkeladresseBostedsadresse(detaljer skjult)"

    override fun tilFrontendString() = """Matrikkel $matrikkelId, bruksenhet $bruksenhetsnummer, postnummer $postnummer${poststed?.let { ", $it" }}""".trimMargin()

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherMatrikkeladresse = other as GrMatrikkeladresseBostedsadresse
        return this === other ||
            (
                matrikkelId != null &&
                    matrikkelId == otherMatrikkeladresse.matrikkelId &&
                    bruksenhetsnummer == otherMatrikkeladresse.bruksenhetsnummer
            )
    }

    override fun hashCode(): Int = Objects.hash(matrikkelId)

    companion object {
        fun fraMatrikkeladresse(
            matrikkeladresse: Matrikkeladresse,
            poststed: String?,
        ): GrMatrikkeladresseBostedsadresse =
            GrMatrikkeladresseBostedsadresse(
                matrikkelId = matrikkeladresse.matrikkelId,
                bruksenhetsnummer = matrikkeladresse.bruksenhetsnummer,
                tilleggsnavn = matrikkeladresse.tilleggsnavn,
                postnummer = matrikkeladresse.postnummer,
                kommunenummer = matrikkeladresse.kommunenummer,
                poststed = poststed,
            )
    }
}
