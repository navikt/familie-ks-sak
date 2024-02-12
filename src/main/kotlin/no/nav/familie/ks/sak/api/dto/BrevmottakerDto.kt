package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType

data class BrevmottakerDto(
    val id: Long?,
    val type: MottakerType,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String? = "",
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)

fun BrevmottakerDto.tilBrevMottakerDb(behandlingId: Long) =
    BrevmottakerDb(
        behandlingId = behandlingId,
        type = type,
        navn = navn,
        adresselinje1 = adresselinje1,
        adresselinje2 = adresselinje2,
        postnummer = postnummer.trim(),
        poststed = poststed.trim(),
        landkode = landkode,
    )
