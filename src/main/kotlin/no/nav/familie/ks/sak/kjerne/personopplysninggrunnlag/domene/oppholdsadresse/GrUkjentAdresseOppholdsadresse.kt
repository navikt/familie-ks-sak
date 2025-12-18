package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.oppholdsadresse

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted.PAA_SVALBARD
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresse

@Entity(name = "GrUkjentAdresseOppholdsadresse")
@DiscriminatorValue("UkjentAdresse")
class GrUkjentAdresseOppholdsadresse : GrOppholdsadresse() {
    override fun toString(): String = "GrUkjentAdresseOppholdsadresse(detaljer skjult)"

    override fun toSecureString(): String = "GrUkjentAdresseOppholdsadresse(${oppholdAnnetSted ?: ""})"

    override fun tilFrontendString(): String = "Ukjent adresse${oppholdAnnetSted.takeIf { it == PAA_SVALBARD }?.let { ", $it" } ?: ""}"

    override fun tilAdresse(): Adresse =
        Adresse(
            gyldigFraOgMed = periode?.fom,
            gyldigTilOgMed = periode?.tom,
            oppholdAnnetSted = oppholdAnnetSted,
        )

    override fun erPåSvalbard(): Boolean = oppholdAnnetSted == PAA_SVALBARD
}
