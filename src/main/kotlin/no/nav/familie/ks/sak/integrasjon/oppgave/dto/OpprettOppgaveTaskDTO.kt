package no.nav.familie.ks.sak.integrasjon.oppgave.dto

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDate

data class OpprettOppgaveTaskDTO(
    val behandlingId: Long,
    val oppgavetype: Oppgavetype,
    val fristForFerdigstillelse: LocalDate,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String?
)
