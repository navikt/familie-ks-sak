package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppgaveService(private val integrasjonService: IntegrasjonService) {

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String, overstyrFordeling: Boolean = false): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonService.finnOppgaveMedId(oppgaveId)

            if (!oppgave.tilordnetRessurs.isNullOrEmpty()) {
                throw FunksjonellFeil(
                    melding = "Oppgaven er allerede fordelt",
                    frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}"
                )
            }
        }

        return integrasjonService.fordelOppgave(oppgaveId, saksbehandler).oppgaveId.toString()
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonService.fordelOppgave(oppgaveId, null)
        return integrasjonService.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgave(oppgaveId: Long): Oppgave = integrasjonService.finnOppgaveMedId(oppgaveId)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto =
        integrasjonService.hentOppgaver(finnOppgaveRequest)

    fun ferdigstillOppgave(oppgave: Oppgave) {
        val oppgaveId = oppgave.id

        requireNotNull(oppgaveId) { "Oppgaven må ha en id for å kunne ferdigstilles" }

        integrasjonService.ferdigstillOppgave(oppgaveId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLoger")
    }
}
