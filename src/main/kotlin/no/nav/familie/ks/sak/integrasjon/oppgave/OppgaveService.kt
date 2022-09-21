package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.DbOppgave
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.OppgaveRepository
import no.nav.familie.ks.sak.kjerne.behandling.Behandling
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OppgaveService(
    private val integrasjonClient: IntegrasjonClient,
    private val oppgaveRepository: OppgaveRepository
) {

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String, overstyrFordeling: Boolean = false): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)

            if (!oppgave.tilordnetRessurs.isNullOrEmpty()) {
                throw FunksjonellFeil(
                    melding = "Oppgaven er allerede fordelt",
                    frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}"
                )
            }
        }

        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler).oppgaveId.toString()
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonClient.fordelOppgave(oppgaveId, null)
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgave(oppgaveId: Long): Oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto =
        integrasjonClient.hentOppgaver(finnOppgaveRequest)

    fun hentOppgaverSomIkkeErFerdigstilt(behandling: Behandling): List<DbOppgave> =
        oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)

    fun ferdigstillOppgave(oppgave: Oppgave) {
        val oppgaveId = oppgave.id
        requireNotNull(oppgaveId) { "Oppgaven må ha en id for å kunne ferdigstilles" }
        integrasjonClient.ferdigstillOppgave(oppgaveId)
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse = integrasjonClient.patchOppgave(patchOppgave)

    fun patchOppgaverForBehandling(behandling: Behandling, copyOppgave: (oppgave: Oppgave) -> Oppgave?) {
        hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
            val oppgave = hentOppgave(dbOppgave.gsakId.toLong())
            copyOppgave(oppgave)?.also { patchOppgave(it) }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLoger")
    }
}
