package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene


import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.time.LocalDate

/**
 * NB: Bør ikke brukes internt, men kun ut mot eksterne tjenester siden klassen
 * inneholder aktiv personIdent og ikke aktørId.
 */
data class MinimertRestPerson(
    val personIdent: String,
    val fødselsdato: LocalDate,
    val type: PersonType
) {
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)
}

fun PersonResponsDto.tilMinimertRestPerson() = MinimertRestPerson(
    personIdent = this.personIdent,
    fødselsdato = fødselsdato ?: throw Feil("Fødselsdato mangler"),
    type = this.type
)

fun List<MinimertRestPerson>.barnMedSeksårsdagPåFom(fom: LocalDate?): List<MinimertRestPerson> {
    return this
        .filter { it.type == PersonType.BARN }
        .filter { person ->
            person.hentSeksårsdag().toYearMonth() == (
                fom?.toYearMonth()
                    ?: TIDENES_ENDE.toYearMonth()
                )
        }
}

fun Person.tilMinimertPerson() = MinimertPerson(
    aktivPersonIdent = this.aktør.aktivFødselsnummer(),
    aktørId = this.aktør.aktørId,
    fødselsdato = this.fødselsdato,
    type = this.type,
    dødsfallsdato = this.dødsfall?.dødsfallDato
)

fun Person.tilMinimertRestPerson() = MinimertRestPerson(
    personIdent = this.aktør.aktivFødselsnummer(),
    fødselsdato = this.fødselsdato,
    type = this.type
)

data class MinimertUregistrertBarn(
    val personIdent: String,
    val navn: String,
    val fødselsdato: LocalDate? = null
)

fun BarnMedOpplysningerDto.tilMinimertUregisrertBarn() = MinimertUregistrertBarn(
    personIdent = this.ident,
    navn = this.navn,
    fødselsdato = this.fødselsdato
)