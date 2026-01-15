package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser

import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person

data class Adresser(
    val bostedsadresser: List<Adresse>,
    val oppholdsadresse: List<Adresse>,
) {
    companion object {
        fun opprettFra(person: Person): Adresser =
            Adresser(
                bostedsadresser = person.bostedsadresser.map { it.tilAdresse() },
                oppholdsadresse = person.oppholdsadresser.map { it.tilAdresse() },
            )
    }
}
