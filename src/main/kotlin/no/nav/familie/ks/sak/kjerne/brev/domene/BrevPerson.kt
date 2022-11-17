package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.common.util.slåSammen
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate

class BrevPerson(
    val type: PersonType,
    val fødselsdato: LocalDate,
    val aktørId: String,
    val aktivPersonIdent: String,
    val dødsfallsdato: LocalDate?
) {
    fun erDød() = dødsfallsdato != null
}

fun Person.tilBrevPerson() = BrevPerson(
    aktivPersonIdent = this.aktør.aktivFødselsnummer(),
    aktørId = this.aktør.aktørId,
    fødselsdato = this.fødselsdato,
    type = this.type,
    dødsfallsdato = this.dødsfall?.dødsfallDato
)

fun PersonopplysningGrunnlag.tilBrevPersoner(): List<BrevPerson> = personer.tilBrevPersoner()

fun Set<Person>.tilBrevPersoner(): List<BrevPerson> = map {
    BrevPerson(
        it.type,
        it.fødselsdato,
        it.aktør.aktørId,
        it.aktør.aktivFødselsnummer(),
        it.dødsfall?.dødsfallDato
    )
}

fun List<BrevPerson>.tilBarnasFødselsdatoer(): String =
    slåSammen(
        this
            .filter { it.type == PersonType.BARN }
            .sortedBy { person ->
                person.fødselsdato
            }
            .map { person ->
                person.fødselsdato.tilKortString()
            }
    )
