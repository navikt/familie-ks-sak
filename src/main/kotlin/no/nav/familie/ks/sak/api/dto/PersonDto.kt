package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.ks.sak.integrasjon.pdl.domene.FalskIdentitetPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PersonInfo
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import java.time.LocalDate

data class PersonInfoDto(
    val personIdent: String,
    var fødselsdato: LocalDate? = null,
    val navn: String? = null,
    val kjønn: Kjønn? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    var harTilgang: Boolean = true,
    val forelderBarnRelasjon: List<ForelderBarnRelasjonInfoDto> = emptyList(),
    val forelderBarnRelasjonMaskert: List<ForelderBarnRelasjonInfoMaskertDto> = emptyList(),
    val kommunenummer: String = "ukjent",
    val dødsfallDato: String? = null,
    val bostedsadresse: BostedsadresseDto? = null,
    val erEgenAnsatt: Boolean? = null,
    val harFalskIdentitet: Boolean = false,
)

data class ForelderBarnRelasjonInfoDto(
    val personIdent: String,
    val relasjonRolle: FORELDERBARNRELASJONROLLE,
    val navn: String,
    val fødselsdato: LocalDate?,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    val erEgenAnsatt: Boolean? = null,
)

data class ForelderBarnRelasjonInfoMaskertDto(
    val relasjonRolle: FORELDERBARNRELASJONROLLE,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING,
)

data class BostedsadresseDto(
    val adresse: String?,
    val postnummer: String,
)

fun PdlPersonInfo.tilPersonInfoDto(personIdent: String): PersonInfoDto =
    when (this) {
        is PdlPersonInfo.Person -> {
            this.personInfo.tilPersonInfoDto(personIdent)
        }

        is PdlPersonInfo.FalskPerson -> {
            this.falskIdentitetPersonInfo.tilPersonInfoDto(personIdent)
        }
    }

fun PersonInfo.tilPersonInfoDto(personIdent: String): PersonInfoDto {
    val bostedsadresse =
        this.bostedsadresser.filter { it.angittFlyttedato != null }.maxByOrNull { it.angittFlyttedato!! }

    val kommunenummer =
        when {
            bostedsadresse == null -> null
            bostedsadresse.vegadresse != null -> bostedsadresse.vegadresse?.kommunenummer
            bostedsadresse.matrikkeladresse != null -> bostedsadresse.matrikkeladresse?.kommunenummer
            bostedsadresse.ukjentBosted != null -> null
            else -> null
        } ?: "ukjent"

    val dødsfallDato = if (this.dødsfall != null && this.dødsfall.erDød) this.dødsfall.dødsdato else null

    return PersonInfoDto(
        personIdent = personIdent,
        fødselsdato = this.fødselsdato,
        navn = this.navn,
        kjønn = this.kjønn,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering,
        forelderBarnRelasjon = this.forelderBarnRelasjoner.map { it.tilForelderBarnRelasjonInfoDto() },
        forelderBarnRelasjonMaskert = this.forelderBarnRelasjonerMaskert.map { it.tilForelderBarnRelasjonInfoMaskertDto() },
        kommunenummer = kommunenummer,
        dødsfallDato = dødsfallDato,
        erEgenAnsatt = this.erEgenAnsatt,
    )
}

fun FalskIdentitetPersonInfo.tilPersonInfoDto(personIdent: String): PersonInfoDto {
    val nyesteAdresse = adresser?.bostedsadresser?.filter { it.gyldigFraOgMed != null }?.maxByOrNull { it.gyldigFraOgMed!! }
    val kommunenummer =
        when {
            nyesteAdresse?.vegadresse != null -> nyesteAdresse.vegadresse.kommunenummer
            nyesteAdresse?.matrikkeladresse != null -> nyesteAdresse.matrikkeladresse.kommunenummer
            else -> "ukjent"
        } ?: "ukjent"

    return PersonInfoDto(
        personIdent = personIdent,
        navn = this.navn,
        fødselsdato = this.fødselsdato,
        kjønn = this.kjønn,
        kommunenummer = kommunenummer,
        harFalskIdentitet = true,
    )
}

private fun ForelderBarnRelasjonInfoMaskert.tilForelderBarnRelasjonInfoMaskertDto() =
    ForelderBarnRelasjonInfoMaskertDto(
        relasjonRolle = this.relasjonsrolle,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering,
    )

private fun ForelderBarnRelasjonInfo.tilForelderBarnRelasjonInfoDto() =
    ForelderBarnRelasjonInfoDto(
        personIdent = this.aktør.aktivFødselsnummer(),
        relasjonRolle = this.relasjonsrolle,
        navn = this.navn ?: "",
        fødselsdato = this.fødselsdato,
        adressebeskyttelseGradering = this.adressebeskyttelseGradering,
        erEgenAnsatt = this.erEgenAnsatt,
    )
