package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

data class OpprettOppgaveDto(
    val fnr: String,
    val oppgavetype: Oppgavetype,
    val enhet: String
)
