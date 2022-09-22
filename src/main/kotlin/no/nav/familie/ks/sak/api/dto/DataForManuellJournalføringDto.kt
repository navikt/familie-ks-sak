package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgave

data class DataForManuellJournalføringDto(
    val oppgave: Oppgave,
    val person: PersonInfoDto?,
    val journalpost: Journalpost?,
    val minimalFagsak: MinimalFagsakResponsDto?
)
