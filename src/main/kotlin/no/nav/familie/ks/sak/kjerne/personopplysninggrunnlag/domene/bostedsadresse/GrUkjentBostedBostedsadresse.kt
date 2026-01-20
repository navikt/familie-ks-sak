package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresse

@Entity(name = "GrUkjentBostedBostedsadresse")
@DiscriminatorValue("ukjentBosted")
data class GrUkjentBostedBostedsadresse(
    @Column(name = "bostedskommune")
    val bostedskommune: String,
) : GrBostedsadresse() {
    override fun toSecureString(): String = """GrUkjentBostedBostedsadresse(bostedskommune=$bostedskommune""".trimMargin()

    override fun tilFrontendString() = """Ukjent adresse, kommune $bostedskommune""".trimMargin()

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            ukjentBosted = UkjentBosted(bostedskommune = bostedskommune),
        )

    override fun toString(): String = "GrUkjentBostedBostedsadresse(detaljer skjult)"

    companion object {
        fun fraUkjentBosted(ukjentBosted: UkjentBosted): GrUkjentBostedBostedsadresse = GrUkjentBostedBostedsadresse(bostedskommune = ukjentBosted.bostedskommune)
    }
}
