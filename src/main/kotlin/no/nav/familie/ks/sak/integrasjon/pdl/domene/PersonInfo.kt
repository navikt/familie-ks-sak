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
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.adresser.Adresser
import java.time.LocalDate
import java.time.Period

sealed class PdlPersonInfo {
    data class Person(
        val personInfo: PersonInfo,
    ) : PdlPersonInfo()

    data class FalskPerson(
        val falskIdentitetPersonInfo: FalskIdentitetPersonInfo,
    ) : PdlPersonInfo()

    fun personInfoBase(): PersonInfoBase =
        when (this) {
            is Person -> this.personInfo
            is FalskPerson -> this.falskIdentitetPersonInfo
        }
}

interface PersonInfoBase {
    val fødselsdato: LocalDate?
    val navn: String?
    val kjønn: Kjønn
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING?
    val erEgenAnsatt: Boolean?
    val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo>

    fun erBarn(): Boolean = fødselsdato?.let { Period.between(fødselsdato, LocalDate.now()).years < 18 } ?: false
}

data class PersonInfo(
    override val fødselsdato: LocalDate,
    override val navn: String? = null,
    @JsonDeserialize(using = KjoennDeserializer::class)
    override val kjønn: Kjønn = Kjønn.UKJENT,
    // Observer at ForelderBarnRelasjon og ForelderBarnRelasjonMaskert ikke er en PDL-objekt.
    override val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> = emptySet(),
    val forelderBarnRelasjonerMaskert: Set<ForelderBarnRelasjonInfoMaskert> = emptySet(),
    override val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val bostedsadresser: List<Bostedsadresse> = emptyList(),
    val oppholdsadresser: List<Oppholdsadresse> = emptyList(),
    val sivilstander: List<Sivilstand> = emptyList(),
    val opphold: List<Opphold>? = emptyList(),
    val statsborgerskap: List<Statsborgerskap>? = emptyList(),
    val dødsfall: DødsfallData? = null,
    val kontaktinformasjonForDoedsbo: PdlKontaktinformasjonForDødsbo? = null,
    override val erEgenAnsatt: Boolean? = null,
) : PersonInfoBase

data class FalskIdentitetPersonInfo(
    override val navn: String? = "Ukjent navn",
    override val fødselsdato: LocalDate? = null,
    override val kjønn: Kjønn = Kjønn.UKJENT,
    val adresser: Adresser? = null,
) : PersonInfoBase {
    override val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null
    override val erEgenAnsatt: Boolean? = null
    override val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> = emptySet()
}

fun List<Bostedsadresse>.filtrerUtKunNorskeBostedsadresser() = this.filter { it.vegadresse != null || it.matrikkeladresse != null || it.ukjentBosted != null }

data class ForelderBarnRelasjonInfo(
    val aktør: Aktør,
    val relasjonsrolle: FORELDERBARNRELASJONROLLE,
    override val navn: String? = null,
    override val fødselsdato: LocalDate? = null,
    override val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    override val erEgenAnsatt: Boolean? = null,
    override val kjønn: Kjønn = Kjønn.UKJENT,
) : PersonInfoBase {
    override val forelderBarnRelasjoner: Set<ForelderBarnRelasjonInfo> = emptySet()

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
