package no.nav.familie.ks.sak.integrasjon.journalføring.domene

import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING

data class TilgangsstyrtJournalpost(
    val journalpost: Journalpost,
    val harTilgang: Boolean,
    val adressebeskyttelsegradering: ADRESSEBESKYTTELSEGRADERING?,
)
