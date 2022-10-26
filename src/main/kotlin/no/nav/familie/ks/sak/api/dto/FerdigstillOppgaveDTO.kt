package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

data class FerdigstillOppgaveDTO(
    val behandlingId: Long,
    val oppgavetype: Oppgavetype
)
