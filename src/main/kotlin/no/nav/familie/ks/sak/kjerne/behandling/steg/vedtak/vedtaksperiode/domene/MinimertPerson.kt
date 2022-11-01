package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate

class MinimertPerson(
    val type: PersonType,
    val fødselsdato: LocalDate,
    val aktørId: String,
    val aktivPersonIdent: String,
    val dødsfallsdato: LocalDate?
) {
    val erDød = {
        dødsfallsdato != null
    }
    fun hentSeksårsdag(): LocalDate = fødselsdato.plusYears(6)

    fun tilMinimertRestPerson() = MinimertRestPerson(
        personIdent = aktivPersonIdent,
        fødselsdato = fødselsdato,
        type = type
    )
}

fun PersonopplysningGrunnlag.tilMinimertePersoner(): List<MinimertPerson> =
    this.personer.tilMinimertePersoner()

fun Set<Person>.tilMinimertePersoner(): List<MinimertPerson> =
    this.map {
        MinimertPerson(
            it.type,
            it.fødselsdato,
            it.aktør.aktørId,
            it.aktør.aktivFødselsnummer(),
            it.dødsfall?.dødsfallDato
        )
    }

fun List<MinimertPerson>.harBarnMedSeksårsdagPåFom(fom: LocalDate?) = this.any { person ->
    person
        .hentSeksårsdag()
        .toYearMonth() == (fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth())
}
