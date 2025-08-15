package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDate
import java.time.LocalDateTime

data class FinnOppgaveDto(
    val behandlingstema: String?,
    val behandlingstype: String?,
    val oppgavetype: String?,
    val enhet: String?,
    val saksbehandler: String?,
    val journalpostId: String?,
    val tilordnetRessurs: String?,
    val tildeltRessurs: Boolean?,
    val opprettetFomTidspunkt: LocalDateTime?,
    val opprettetTomTidspunkt: LocalDateTime?,
    val fristFomDato: LocalDate?,
    val fristTomDato: LocalDate?,
    val aktivFomDato: LocalDate?,
    val aktivTomDato: LocalDate?,
    val limit: Long?,
    val offset: Long?,
) {
    fun tilFinnOppgaveRequest(): FinnOppgaveRequest =
        FinnOppgaveRequest(
            tema = Tema.KON,
            behandlingstema = Behandlingstema.entries.find { it.value == this.behandlingstema },
            behandlingstype = Behandlingstype.entries.find { it.value == this.behandlingstype },
            oppgavetype = Oppgavetype.entries.find { it.value == this.oppgavetype },
            enhet = this.enhet,
            saksbehandler = this.saksbehandler,
            journalpostId = this.journalpostId,
            tildeltRessurs = this.tildeltRessurs,
            tilordnetRessurs = this.tilordnetRessurs,
            opprettetFomTidspunkt = this.opprettetFomTidspunkt,
            opprettetTomTidspunkt = this.opprettetTomTidspunkt,
            fristFomDato = this.fristFomDato,
            fristTomDato = this.fristTomDato,
            aktivFomDato = this.aktivFomDato,
            aktivTomDato = this.aktivTomDato,
            limit = this.limit,
            offset = this.offset,
        )
}
