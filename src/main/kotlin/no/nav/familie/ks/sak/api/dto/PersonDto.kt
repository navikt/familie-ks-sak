package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfoMaskert
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import java.time.LocalDate

data class PersonInfoDto(
    val personIdent: String,
    var fødselsdato: LocalDate? = null,
    val navn: String? = null,
    val kjønn: KJOENN? = null,
    val adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
    var harTilgang: Boolean = true,
    val forelderBarnRelasjon: List<ForelderBarnRelasjonInfoDto> = emptyList(),
    val forelderBarnRelasjonMaskert: List<ForelderBarnRelasjonInfoMaskertDto> = emptyList(),
    val kommunenummer: String = "ukjent",
    val dødsfallDato: String? = null,
    val bostedsadresse: BostedsadresseDto? = null,
    val erEgenAnsatt: Boolean? = null,
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

fun PdlPersonInfo.tilPersonInfoDto(personIdent: String): PersonInfoDto {
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
    )

fun PersonInfoDto.leggTilEgenAnsattStatus(integrasjonService: IntegrasjonService): PersonInfoDto {
    val personIdenter = this.forelderBarnRelasjon.map { it.personIdent } + this.personIdent
    val erEgenAnsattMap = integrasjonService.sjekkErEgenAnsattBulk(personIdenter)
    return this.copy(
        erEgenAnsatt = erEgenAnsattMap.getOrDefault(this.personIdent, null),
        forelderBarnRelasjon =
            this.forelderBarnRelasjon.map {
                it.copy(erEgenAnsatt = erEgenAnsattMap.getOrDefault(it.personIdent, null))
            },
    )
}
