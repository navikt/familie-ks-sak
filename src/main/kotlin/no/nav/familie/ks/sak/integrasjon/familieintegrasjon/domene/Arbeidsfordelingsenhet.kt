package no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene

import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet

data class Arbeidsfordelingsenhet(
    val enhetId: String,
    val enhetNavn: String,
) {
    companion object {
        fun opprettFra(enhet: KontantstøtteEnhet): Arbeidsfordelingsenhet =
            Arbeidsfordelingsenhet(
                enhetId = enhet.enhetsnummer,
                enhetNavn = enhet.enhetsnavn,
            )
    }
}
