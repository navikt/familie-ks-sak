package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND

fun finnPersonMedStrengesteAdressebeskyttelse(personer: List<Pair<String, ADRESSEBESKYTTELSEGRADERING?>>): String? {
    return personer.fold(
        null,
        @Suppress("ktlint:standard:blank-line-before-declaration")
        fun(
            person: Pair<String, ADRESSEBESKYTTELSEGRADERING?>?,
            neste: Pair<String, ADRESSEBESKYTTELSEGRADERING?>,
        ): Pair<String, ADRESSEBESKYTTELSEGRADERING?>? {
            return when {
                person?.second == STRENGT_FORTROLIG -> person
                neste.second == STRENGT_FORTROLIG -> neste
                person?.second == STRENGT_FORTROLIG_UTLAND -> person
                neste.second == STRENGT_FORTROLIG_UTLAND -> neste
                person?.second == FORTROLIG -> person
                neste.second == FORTROLIG -> neste
                else -> null
            }
        },
    )?.first
}
