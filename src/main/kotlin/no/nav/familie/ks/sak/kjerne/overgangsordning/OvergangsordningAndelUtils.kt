package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.common.util.toYearMonth
import java.time.LocalDate
import java.time.YearMonth

fun beregnGyldigFom(fødselsdato: LocalDate): YearMonth = fødselsdato.plusMonths(20).toYearMonth()

fun beregnGyldigTom(fødselsdato: LocalDate): YearMonth = fødselsdato.plusMonths(23).toYearMonth()
