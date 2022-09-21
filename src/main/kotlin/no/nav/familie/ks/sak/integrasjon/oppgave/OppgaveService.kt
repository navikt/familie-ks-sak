package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.DbOppgave
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.OppgaveRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
    private val integrasjonClient: IntegrasjonClient,
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingRepository: BehandlingRepository,
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository
) {

    fun opprettOppgave(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        fristForFerdigstillelse: LocalDate,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null
    ): String {
        val behandling = behandlingRepository.finnBehandling(behandlingId)
        val eksisterendeOppgave =
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(oppgavetype, behandling)
        if (eksisterendeOppgave != null && oppgavetype != Oppgavetype.Journalføring) {
            logger.warn(
                "Fant eksisterende oppgave med samme oppgavetype som ikke er ferdigstilt " +
                    "ved opprettelse av ny oppgave $eksisterendeOppgave. " +
                    "Vi oppretter ikke ny oppgave, men gjenbruker eksisterende."
            )
            return eksisterendeOppgave.gsakId
        }
        val arbeidsfordelingsenhet: ArbeidsfordelingPåBehandling? =
            arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id)

        if (arbeidsfordelingsenhet == null) {
            logger.warn("Fant ikke behandlende enhet på behandling ${behandling.id} ved opprettelse av $oppgavetype-oppgave.")
        }

        val opprettOppgaveRequest = OpprettOppgaveRequest(
            ident = OppgaveIdentV2(ident = behandling.fagsak.aktør.aktørId, gruppe = IdentGruppe.AKTOERID),
            saksId = behandling.fagsak.id.toString(),
            tema = Tema.KON,
            oppgavetype = oppgavetype,
            fristFerdigstillelse = fristForFerdigstillelse,
            beskrivelse = lagOppgaveTekst(behandling.fagsak.id, beskrivelse),
            enhetsnummer = arbeidsfordelingsenhet?.behandlendeEnhetId,
            behandlingstema = Behandlingstema.Kontantstøtte.value,
            // TODO - må diskuteres hva det kan være for KS-EØS
            behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
            tilordnetRessurs = tilordnetNavIdent
        )
        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgaveRequest).oppgaveId.toString()

        val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
        oppgaveRepository.save(oppgave)

        return opprettetOppgaveId
    }

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

    private fun lagOppgaveTekst(fagsakId: Long, beskrivelse: String? = null): String {
        return beskrivelse?.let { it + "\n" }
            ?: (
                "----- Opprettet av familie-ba-sak ${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)} --- \n" +
                    "https://ks.intern.nav.no/fagsak/$fagsakId"
                )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLoger")
    }
}
