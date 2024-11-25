package no.nav.familie.ks.sak.integrasjon.oppgave

import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleConfig
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.DbOppgave
import no.nav.familie.ks.sak.integrasjon.oppgave.domene.OppgaveRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.hentArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.tilArbeidsfordelingsenhet
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.unleash.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

@Service
class OppgaveService(
    private val integrasjonClient: IntegrasjonClient,
    private val oppgaveRepository: OppgaveRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService,
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    private val unleashService: UnleashService,
) {
    fun opprettOppgave(
        behandlingId: Long,
        oppgavetype: Oppgavetype,
        fristForFerdigstillelse: LocalDate,
        tilordnetNavIdent: String? = null,
        beskrivelse: String? = null,
    ): String {
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        val eksisterendeIkkeFerdigstiltOppgave =
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                oppgavetype,
                behandling,
            )

        if (eksisterendeIkkeFerdigstiltOppgave != null && oppgavetype != Oppgavetype.Journalføring) {
            logger.warn(
                "Fant eksisterende oppgave $eksisterendeIkkeFerdigstiltOppgave med " +
                    "samme oppgavetype $oppgavetype som ikke er ferdigstilt for behandling ${behandling.id}." +
                    "Vi oppretter ikke ny oppgave, men gjenbruker eksisterende.",
            )
            return eksisterendeIkkeFerdigstiltOppgave.gsakId
        }

        val arbeidsfordelingsenhet =
            arbeidsfordelingPåBehandlingRepository
                .hentArbeidsfordelingPåBehandling(behandlingId)
                .tilArbeidsfordelingsenhet()

        val opprettSakPåRiktigEnhetOgSaksbehandlerToggleErPå = unleashService.isEnabled(FeatureToggleConfig.OPPRETT_SAK_PÅ_RIKTIG_ENHET_OG_SAKSBEHANDLER, false)

        val navIdent = tilordnetNavIdent?.let { NavIdent(it) }
        val tilordnetRessurs =
            if (opprettSakPåRiktigEnhetOgSaksbehandlerToggleErPå) {
                tilpassArbeidsfordelingService.bestemTilordnetRessursPåOppgave(arbeidsfordelingsenhet, navIdent)
            } else {
                navIdent
            }

        val opprettOppgaveRequest =
            OpprettOppgaveRequest(
                ident = OppgaveIdentV2(ident = behandling.fagsak.aktør.aktørId, gruppe = IdentGruppe.AKTOERID),
                saksId = behandling.fagsak.id.toString(),
                tema = Tema.KON,
                oppgavetype = oppgavetype,
                fristFerdigstillelse = fristForFerdigstillelse,
                beskrivelse = lagOppgaveTekst(behandling.fagsak.id, beskrivelse),
                enhetsnummer = arbeidsfordelingsenhet.enhetId,
                // behandlingstema brukes ikke i kombinasjon med behandlingstype for kontantstøtte
                behandlingstema = null,
                // TODO - må diskuteres hva det kan være for KS-EØS
                behandlingstype = behandling.kategori.tilOppgavebehandlingType().value,
                tilordnetRessurs = tilordnetRessurs?.ident,
            )

        val opprettetOppgaveId = integrasjonClient.opprettOppgave(opprettOppgaveRequest).oppgaveId.toString()

        val oppgave = DbOppgave(gsakId = opprettetOppgaveId, behandling = behandling, type = oppgavetype)
        oppgaveRepository.save(oppgave)

        return opprettetOppgaveId
    }

    fun fordelOppgave(
        oppgaveId: Long,
        saksbehandler: String,
        overstyrFordeling: Boolean = false,
    ): String {
        if (!overstyrFordeling) {
            val oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)

            if (!oppgave.tilordnetRessurs.isNullOrEmpty()) {
                throw FunksjonellFeil(
                    melding = "Oppgaven er allerede fordelt",
                    frontendFeilmelding = "Oppgaven er allerede fordelt til ${oppgave.tilordnetRessurs}",
                )
            }
        }

        return integrasjonClient.fordelOppgave(oppgaveId, saksbehandler).oppgaveId.toString()
    }

    fun ferdigstillOppgaver(
        behandling: Behandling,
        oppgavetype: Oppgavetype,
    ) {
        val oppgaverSomSkalFerdigstilles =
            oppgaveRepository.finnOppgaverSomSkalFerdigstilles(oppgavetype, behandling)

        oppgaverSomSkalFerdigstilles.forEach {
            try {
                integrasjonClient.ferdigstillOppgave(it.gsakId.toLong())

                it.erFerdigstilt = true
                oppgaveRepository.saveAndFlush(it)
            } catch (exception: Exception) {
                throw Feil(message = "Klarte ikke å ferdigstille oppgave med id ${it.gsakId}.", cause = exception)
            }
        }
    }

    fun tilbakestillFordelingPåOppgave(oppgaveId: Long): Oppgave {
        integrasjonClient.fordelOppgave(oppgaveId, null)
        return integrasjonClient.finnOppgaveMedId(oppgaveId)
    }

    fun hentOppgave(oppgaveId: Long): Oppgave = integrasjonClient.finnOppgaveMedId(oppgaveId)

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto = integrasjonClient.hentOppgaver(finnOppgaveRequest)

    fun hentOppgaverSomIkkeErFerdigstilt(behandling: Behandling): List<DbOppgave> =
        oppgaveRepository.findByBehandlingAndIkkeFerdigstilt(behandling)

    fun ferdigstillOppgave(oppgave: Oppgave) {
        val oppgaveId = oppgave.id
        requireNotNull(oppgaveId) { "Oppgaven må ha en id for å kunne ferdigstilles" }
        integrasjonClient.ferdigstillOppgave(oppgaveId)
    }

    fun forlengFristÅpneOppgaverPåBehandling(
        behandlingId: Long,
        forlengelse: Period,
    ) {
        val dbOppgaver = oppgaveRepository.findByBehandlingIdAndIkkeFerdigstilt(behandlingId)

        dbOppgaver.forEach { dbOppgave ->
            val gammelOppgave = hentOppgave(dbOppgave.gsakId.toLong())
            val oppgaveErAvsluttet = gammelOppgave.ferdigstiltTidspunkt != null

            when {
                gammelOppgave.id == null ->
                    logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")

                gammelOppgave.fristFerdigstillelse == null ->
                    logger.warn("Oppgave ${dbOppgave.gsakId} har ingen oppgavefrist ved oppdatering av frist")

                oppgaveErAvsluttet ->
                    logger.warn("Oppgave ${dbOppgave.gsakId} er allerede avsluttet. Frist ikke forlenget.")

                else -> {
                    val nyFrist = LocalDate.parse(gammelOppgave.fristFerdigstillelse).plus(forlengelse)
                    val oppgaveOppdatering = gammelOppgave.copy(fristFerdigstillelse = nyFrist?.toString())
                    integrasjonClient.oppdaterOppgave(oppgaveOppdatering)
                }
            }
        }
    }

    fun settFristÅpneOppgaverPåBehandlingTil(
        behandlingId: Long,
        nyFrist: LocalDate,
    ) {
        val dbOppgaver = oppgaveRepository.findByBehandlingIdAndIkkeFerdigstilt(behandlingId)

        dbOppgaver.forEach { dbOppgave ->
            val gammelOppgave = hentOppgave(dbOppgave.gsakId.toLong())
            val oppgaveErAvsluttet = gammelOppgave.ferdigstiltTidspunkt != null

            when {
                gammelOppgave.id == null -> logger.warn("Finner ikke oppgave ${dbOppgave.gsakId} ved oppdatering av frist")
                oppgaveErAvsluttet ->
                    logger.warn("Oppgave ${dbOppgave.gsakId} er allerede avsluttet. Frist ikke satt.")

                else -> {
                    val oppgaveOppdatering = gammelOppgave.copy(fristFerdigstillelse = nyFrist.toString())
                    integrasjonClient.oppdaterOppgave(oppgaveOppdatering = oppgaveOppdatering)
                }
            }
        }
    }

    fun endreTilordnetEnhetPåOppgaverForBehandling(
        behandling: Behandling,
        nyEnhet: String,
    ) {
        hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
            val oppgave = hentOppgave(dbOppgave.gsakId.toLong())
            logger.info("Oppdaterer enhet fra ${oppgave.tildeltEnhetsnr} til $nyEnhet på oppgave ${oppgave.id}")
            integrasjonClient.tilordneEnhetOgRessursForOppgave(oppgaveId = oppgave.id!!, nyEnhet = nyEnhet)
        }
    }

    fun oppdaterBehandlingstemaPåOppgaverFraBehandling(
        behandling: Behandling,
    ) =
        hentOppgaverSomIkkeErFerdigstilt(behandling).forEach { dbOppgave ->
            val oppgave = hentOppgave(dbOppgave.gsakId.toLong())
            integrasjonClient.oppdaterOppgave(oppgave.copy(behandlingstype = behandling.kategori.tilOppgavebehandlingType().value))
        }

    private fun lagOppgaveTekst(
        fagsakId: Long,
        beskrivelse: String? = null,
    ): String =
        beskrivelse?.let { it + "\n" }
            ?: (
                "----- Opprettet av familie-ks-sak ${
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                } --- \n" +
                    "https://ks.intern.nav.no/fagsak/$fagsakId"
            )

    companion object {
        private val logger = LoggerFactory.getLogger(OppgaveService::class.java)
    }
}
