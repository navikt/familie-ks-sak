package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse

import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import javax.persistence.Column
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity(name = "GrUkjentBosted")
@DiscriminatorValue("ukjentBosted")
data class GrUkjentBosted(
    @Column(name = "bostedskommune")
    val bostedskommune: String

) : GrBostedsadresse() {

    override fun toSecureString(): String = """UkjentadresseDao(bostedskommune=$bostedskommune""".trimMargin()

    override fun tilFrontendString() = """Ukjent adresse, kommune $bostedskommune""".trimMargin()

    override fun toString(): String = "UkjentBostedAdresse(detaljer skjult)"

    companion object {

        fun fraUkjentBosted(ukjentBosted: UkjentBosted): GrUkjentBosted =
            GrUkjentBosted(bostedskommune = ukjentBosted.bostedskommune)
    }
}
