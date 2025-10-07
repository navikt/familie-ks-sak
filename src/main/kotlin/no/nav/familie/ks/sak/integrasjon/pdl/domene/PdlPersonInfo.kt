package no.nav.familie.ks.sak.integrasjon.pdl.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.LocalDate

data class PdlPersonInfo(
    val fødselsdato: LocalDate,
    val navn: String? = null,
    @JsonDeserialize(using = KjoennDeserializer::class)
    val kjønn: KJOENN? = null,
    // Observer at ForelderBarnRelasjon og ForelderBarnRelasjonMaskert ikke er en PDL-objekt.
    val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> = emptySet(),
    val forelderBarnRelasjonerMaskert: Set<ForelderBarnRelasjonInfoMaskert> = emptySet(),
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val bostedsadresser: List<Bostedsadresse> = emptyList(),
    val oppholdsadresser: List<Oppholdsadresse> = emptyList(),
    val sivilstander: List<Sivilstand> = emptyList(),
    val opphold: List<Opphold>? = emptyList(),
    val statsborgerskap: List<Statsborgerskap>? = emptyList(),
    val dødsfall: DødsfallData? = null,
    val kontaktinformasjonForDoedsbo: PdlKontaktinformasjonForDødsbo? = null,
    val erEgenAnsatt: Boolean? = null,
)

fun List<Bostedsadresse>.filtrerUtKunNorskeBostedsadresser() = this.filter { it.vegadresse != null || it.matrikkeladresse != null || it.ukjentBosted != null }

data class ForelderBarnRelasjonInfo(
    val aktør: Aktør,
    val relasjonsrolle: FORELDERBARNRELASJONROLLE,
    val navn: String? = null,
    val fødselsdato: LocalDate? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val erEgenAnsatt: Boolean? = null,
) {
    override fun toString(): String = "ForelderBarnRelasjon(personIdent=XXX, relasjonsrolle=$relasjonsrolle, navn=XXX, fødselsdato=$fødselsdato)"

    fun toSecureString(): String = "ForelderBarnRelasjon(personIdent=${aktør.aktivFødselsnummer()}, relasjonsrolle=$relasjonsrolle, navn=XXX, fødselsdato=$fødselsdato)"

    fun harForelderRelasjon() =
        this.relasjonsrolle in
            listOf(
                FORELDERBARNRELASJONROLLE.FAR,
                FORELDERBARNRELASJONROLLE.MOR,
                FORELDERBARNRELASJONROLLE.MEDMOR,
            )
}

data class ForelderBarnRelasjonInfoMaskert(
    val relasjonsrolle: FORELDERBARNRELASJONROLLE,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING,
) {
    override fun toString(): String = "ForelderBarnRelasjonMaskert(relasjonsrolle=$relasjonsrolle)"
}

data class DødsfallData(
    val erDød: Boolean,
    val dødsdato: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKontaktinformasjonForDødsbo(
    val adresse: PdlKontaktinformasjonForDødsboAdresse,
)

data class PdlKontaktinformasjonForDødsboAdresse(
    val adresselinje1: String,
    val poststedsnavn: String,
    val postnummer: String,
)

class KjoennDeserializer : StdDeserializer<KJOENN>(KJOENN::class.java) {
    override fun deserialize(
        jp: JsonParser?,
        p1: DeserializationContext?,
    ): KJOENN {
        val node: JsonNode = jp!!.codec.readTree(jp)
        return when (val kjønn = node.asText()) {
            "M" -> KJOENN.MANN
            "K" -> KJOENN.KVINNE
            else -> KJOENN.valueOf(kjønn)
        }
    }
}
