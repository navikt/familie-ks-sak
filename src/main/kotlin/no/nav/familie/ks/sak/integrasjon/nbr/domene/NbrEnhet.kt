package no.nav.familie.ks.sak.integrasjon.nbr.domene

import com.fasterxml.jackson.annotation.JsonProperty

private const val RELASJONSTYPE_ADMINISTRATIV_STRUKTUR = "10"

data class NbrEnhet(
    @JsonProperty("Organisasjonsnummer")
    val organisasjonsnummer: String,
    @JsonProperty("Navn")
    val navn: String,
    @JsonProperty("ErAktiv")
    val erAktiv: Boolean,
    @JsonProperty("ForeldreRelasjoner")
    val foreldreRelasjoner: List<NbrRelasjon>? = null,
)

/**
 * Finner organisasjonsnummeret til bydelen i Oslo som barnehagen hører til.
 *
 * Både kommunale og private barnehager peker på bydelen med relasjonstypen «Administrativ struktur».
 * Returnerer null hvis enheten ikke har nøyaktig én bydel.
 */
fun NbrEnhet.finnBydelOrgNummer(): String? =
    foreldreRelasjoner
        .orEmpty()
        .filter { it.relasjonstype.id == RELASJONSTYPE_ADMINISTRATIV_STRUKTUR }
        .mapNotNull { OsloBydel.fraOrganisasjonsnummer(it.enhet.organisasjonsnummer) }
        .distinct()
        .singleOrNull()
        ?.organisasjonsnummer

data class NbrRelasjon(
    @JsonProperty("Enhet")
    val enhet: NbrRelatertEnhet,
    @JsonProperty("Relasjonstype")
    val relasjonstype: NbrRelasjonstype,
)

data class NbrRelatertEnhet(
    @JsonProperty("Organisasjonsnummer")
    val organisasjonsnummer: String,
    @JsonProperty("Navn")
    val navn: String,
)

data class NbrRelasjonstype(
    @JsonProperty("Id")
    val id: String,
    @JsonProperty("Navn")
    val navn: String,
)
