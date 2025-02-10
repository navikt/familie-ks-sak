package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.storForbokstav

data class Landkode(
    val kode: String,
    val navn: String,
) {
    init {
        if (this.kode.length != 2) {
            throw Feil("Forventer landkode på 'ISO 3166-1 alpha-2'-format")
        }
    }
}

fun String.tilLandNavn(landkoderISO2: Map<String, String>): Landkode {
    val kode = landkoderISO2.entries.find { it.key == this } ?: throw Feil("Fant ikke navn for landkode $this.")

    return Landkode(kode.key, kode.value.storForbokstav())
}
