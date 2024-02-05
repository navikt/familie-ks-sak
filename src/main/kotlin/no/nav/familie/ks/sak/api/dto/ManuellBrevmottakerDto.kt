package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType

data class ManuellBrevmottaker(
    val type: MottakerType,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String? = "",
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)
