package no.nav.familie.ks.sak.sikkerhet

import no.nav.familie.tilgangsmaskin.Avvisningskode

data class PersonTilgang(
    val personIdent: String,
    val avvisning: Avvisning?,
) {
    val harTilgang: Boolean get() = avvisning == null

    override fun toString(): String = "PersonTilgang(personIdent=${"*".repeat(personIdent.length)}, avvisning=$avvisning)"

    data class Avvisning(
        val avvisningskode: Avvisningskode,
        val begrunnelse: String,
    )

    companion object {
        fun medTilgang(personIdent: String) = PersonTilgang(personIdent = personIdent, avvisning = null)

        fun avvist(
            personIdent: String,
            avvisningskode: Avvisningskode,
            begrunnelse: String,
        ) = PersonTilgang(personIdent = personIdent, avvisning = Avvisning(avvisningskode = avvisningskode, begrunnelse = begrunnelse))
    }
}
