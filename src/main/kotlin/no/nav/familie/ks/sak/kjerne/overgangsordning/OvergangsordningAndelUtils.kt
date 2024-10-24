package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.YearMonth

fun beregnGyldigFom(person: Person): YearMonth = person.fødselsdato.plusMonths(20).toYearMonth()

fun beregnGyldigTom(person: Person): YearMonth = person.fødselsdato.plusMonths(23).toYearMonth()
