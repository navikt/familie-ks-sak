package no.nav.familie.ks.sak.datagenerator

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import no.nav.familie.ks.sak.data.randomAktør
import java.time.LocalDate

fun lagTestOppgaveDTO(
    oppgaveId: Long,
    oppgavetype: Oppgavetype = Oppgavetype.Journalføring,
    tildeltRessurs: String? = null,
    tildeltEnhetsnr: String? = "4820",
): Oppgave =
    Oppgave(
        id = oppgaveId,
        aktoerId = randomAktør().aktørId,
        identer = listOf(OppgaveIdentV2("11111111111", IdentGruppe.FOLKEREGISTERIDENT)),
        journalpostId = "1234",
        tildeltEnhetsnr = tildeltEnhetsnr,
        tilordnetRessurs = tildeltRessurs,
        behandlesAvApplikasjon = "FS22",
        beskrivelse = "Beskrivelse for oppgave",
        tema = Tema.KON,
        oppgavetype = oppgavetype.value,
        behandlingstema = Behandlingstema.Kontantstøtte.value,
        behandlingstype = Behandlingstype.NASJONAL.value,
        opprettetTidspunkt =
            LocalDate
                .of(
                    2020,
                    1,
                    1,
                ).toString(),
        fristFerdigstillelse =
            LocalDate
                .of(
                    2020,
                    2,
                    1,
                ).toString(),
        prioritet = OppgavePrioritet.NORM,
        status = StatusEnum.OPPRETTET,
    )
