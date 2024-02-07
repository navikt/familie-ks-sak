package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal

data class DistribuerBrevDto(
    val behandlingId: Long?,
    val journalpostId: String,
    val personIdent: String,
    val brevmal: Brevmal,
    val erManueltSendt: Boolean,
    val manuellAdresseInfo: ManuellAdresseInfo? = null,
)

data class ManuellAdresseInfo(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
)
