package no.nav.familie.ks.sak.integrasjon.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem

data class PdlBaseRespons<T>(
    val data: T,
    val errors: List<PdlError>?
) {

    fun harFeil(): Boolean {
        return !errors.isNullOrEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(
    val message: String,
    val extensions: PdlExtensions?
)

data class PdlExtensions(val code: String?) {

    fun notFound() = code == "not_found"
}

class PdlHentIdenterResponse(val pdlIdenter: PdlIdenter?)

data class PdlIdenter(val identer: List<PdlIdent>)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String
)

class PdlAdressebeskyttelseResponse(val person: PdlAdressebeskyttelsePerson?)
class PdlAdressebeskyttelsePerson(val adressebeskyttelse: List<Adressebeskyttelse>)

data class PdlHentPersonResponse(val person: PdlPersonData?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(
    val folkeregisteridentifikator: List<PdlFolkeregisteridentifikator>,
    val foedsel: List<PdlFødselsDato>,
    val navn: List<PdlNavn> = emptyList(),
    val kjoenn: List<PdlKjoenn> = emptyList(),
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
    val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
    val sivilstand: List<Sivilstand> = emptyList(),
    val bostedsadresse: List<Bostedsadresse>,
    val opphold: List<Opphold> = emptyList(),
    val statsborgerskap: List<Statsborgerskap> = emptyList(),
    val doedsfall: List<Doedsfall> = emptyList(),
    val kontaktinformasjonForDoedsbo: List<PdlKontaktinformasjonForDødsbo> = emptyList()
) {
    fun validerOmPersonKanBehandlesIFagsystem() {
        if (foedsel.isEmpty()) throw PdlPersonKanIkkeBehandlesIFagsystem("mangler fødselsdato")
        if (folkeregisteridentifikator.firstOrNull()?.status == FolkeregisteridentifikatorStatus.OPPHOERT) {
            throw PdlPersonKanIkkeBehandlesIFagsystem("er opphørt")
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFolkeregisteridentifikator(
    val identifikasjonsnummer: String?,
    val status: FolkeregisteridentifikatorStatus,
    val type: FolkeregisteridentifikatorType?
)

enum class FolkeregisteridentifikatorStatus { I_BRUK, OPPHOERT }
enum class FolkeregisteridentifikatorType { FNR, DNR }

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFødselsDato(val foedselsdato: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String
) {

    fun fulltNavn(): String {
        return when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(val kjoenn: KJOENN)

class Doedsfall(val doedsdato: String?)
