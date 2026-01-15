package no.nav.familie.ks.sak.integrasjon.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.kontrakter.felles.personopplysning.Adressebeskyttelse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.ForelderBarnRelasjon
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagSystemÅrsak
import no.nav.familie.ks.sak.common.exception.PdlPersonKanIkkeBehandlesIFagsystem
import no.nav.person.pdl.aktor.v2.Type

data class PdlBaseRespons<T>(
    val data: T,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {
    fun harFeil(): Boolean = !errors.isNullOrEmpty()

    fun harAdvarsel(): Boolean = !extensions?.warnings.isNullOrEmpty()

    fun errorMessages(): String = errors?.joinToString { it.message } ?: ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(
    val message: String,
    val extensions: PdlErrorExtensions?,
)

data class PdlErrorExtensions(
    val code: String?,
) {
    fun notFound() = code == "not_found"
}

data class PdlExtensions(
    val warnings: List<PdlWarning>?,
)

data class PdlWarning(
    val details: Any?,
    val id: String?,
    val message: String?,
    val query: String?,
)

class PdlHentIdenterResponse(
    val pdlIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>,
)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: String,
)

fun List<PdlIdent>.hentAktivAktørId(): String =
    this.singleOrNull { it.gruppe == "AKTORID" && !it.historisk }?.ident
        ?: throw Feil("Finner ikke aktørId i Pdl")

fun List<PdlIdent>.hentAktørIder(): List<String> = filter { it.gruppe == Type.AKTORID.name }.map { it.ident }

fun List<PdlIdent>.hentFødselsnumre(): List<String> = filter { it.gruppe == Type.FOLKEREGISTERIDENT.name }.map { it.ident }

fun List<PdlIdent>.hentAktivFødselsnummer(): String =
    this.singleOrNull { it.gruppe == Type.FOLKEREGISTERIDENT.name && !it.historisk }?.ident
        ?: throw Feil("Finner ikke aktørId i Pdl")

class PdlAdressebeskyttelseResponse(
    val person: PdlAdressebeskyttelsePerson?,
)

class PdlAdressebeskyttelsePerson(
    val adressebeskyttelse: List<Adressebeskyttelse>,
)

class PdlStatsborgerskapResponse(
    val person: PdlStatsborgerskapPerson?,
)

class PdlStatsborgerskapPerson(
    val statsborgerskap: List<Statsborgerskap>,
)

class PdlUtenlandskAdressseResponse(
    val person: PdlUtenlandskAdresssePerson?,
)

class PdlUtenlandskAdresssePerson(
    val bostedsadresse: List<PdlUtenlandskAdresssePersonBostedsadresse>,
)

class PdlUtenlandskAdresssePersonBostedsadresse(
    val utenlandskAdresse: PdlUtenlandskAdresssePersonUtenlandskAdresse?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class PdlUtenlandskAdresssePersonUtenlandskAdresse(
    val landkode: String,
)

data class PdlHentPersonResponse(
    val person: PdlPersonData?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(
    val folkeregisteridentifikator: List<PdlFolkeregisteridentifikator>,
    val foedselsdato: List<PdlFødselsDato>,
    val navn: List<PdlNavn> = emptyList(),
    val kjoenn: List<PdlKjoenn> = emptyList(),
    val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList(),
    val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
    val sivilstand: List<Sivilstand> = emptyList(),
    val bostedsadresse: List<Bostedsadresse>,
    val oppholdsadresse: List<Oppholdsadresse> = emptyList(),
    val opphold: List<Opphold> = emptyList(),
    val statsborgerskap: List<Statsborgerskap> = emptyList(),
    val doedsfall: List<Doedsfall> = emptyList(),
    val kontaktinformasjonForDoedsbo: List<PdlKontaktinformasjonForDødsbo> = emptyList(),
) {
    fun validerOmPersonKanBehandlesIFagsystem() {
        if (foedselsdato.isEmpty()) throw PdlPersonKanIkkeBehandlesIFagsystem(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.MANGLER_FØDSELSDATO)
        if (folkeregisteridentifikator.firstOrNull()?.status == FolkeregisteridentifikatorStatus.OPPHOERT) {
            throw PdlPersonKanIkkeBehandlesIFagsystem(PdlPersonKanIkkeBehandlesIFagSystemÅrsak.OPPHØRT)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFolkeregisteridentifikator(
    val identifikasjonsnummer: String?,
    val status: FolkeregisteridentifikatorStatus,
    val type: FolkeregisteridentifikatorType?,
)

enum class FolkeregisteridentifikatorStatus {
    I_BRUK,
    OPPHOERT,
}

enum class FolkeregisteridentifikatorType {
    FNR,
    DNR,
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFødselsDato(
    val foedselsdato: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
) {
    fun fulltNavn(): String =
        when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjoenn(
    val kjoenn: KJOENN,
)

class Doedsfall(
    val doedsdato: String?,
)

data class PdlBolkRespons<T>(
    val data: PersonBolk<T>?,
    val errors: List<PdlError>?,
    val extensions: PdlExtensions?,
) {
    fun errorMessages(): String = errors?.joinToString { it -> it.message } ?: ""

    fun harAdvarsel(): Boolean = !extensions?.warnings.isNullOrEmpty()
}

data class PersonBolk<T>(
    val personBolk: List<PersonDataBolk<T>>,
)

data class PersonDataBolk<T>(
    val ident: String,
    val code: String,
    val person: T?,
)
