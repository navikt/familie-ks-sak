package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Period

@Service
class StegService(
    private val steg: List<IBehandlingSteg>,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveService: OppgaveService,
    private val loggService: LoggService
) {

    @Transactional
    fun utførSteg(behandlingId: Long, behandledeSteg: BehandlingSteg, behandlingStegDto: BehandlingStegDto? = null) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        val behandledeStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandledeSteg)

        valider(behandling, behandledeSteg)

        when (behandledeStegTilstand.behandlingStegStatus) {
            BehandlingStegStatus.KLAR -> {
                // utfør steg, kaller utfør metode i tilsvarende steg klasser
                behandlingStegDto?.let { hentStegInstans(behandledeSteg).utførSteg(behandlingId, it) }
                    ?: hentStegInstans(behandledeSteg).utførSteg(behandlingId)
                // oppdaterer nåværendeSteg status til utført
                hentStegTilstandForBehandlingSteg(behandling, behandledeSteg).behandlingStegStatus =
                    BehandlingStegStatus.UTFØRT
                // Henter neste steg basert på sekvens og årsak
                val nesteSteg = hentNesteSteg(behandling, behandledeSteg)
                // legger til neste steg hvis steget er ny, eller oppdaterer eksisterende steg status til KLAR
                behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == nesteSteg }
                    ?.let { it.behandlingStegStatus = BehandlingStegStatus.KLAR }
                    ?: behandling.leggTilNesteSteg(nesteSteg)

                // oppdaterer behandling med behandlingstegtilstand og behandling status
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandledeSteg))
            }

            BehandlingStegStatus.UTFØRT -> {
                // tilbakefører alle stegene som er etter behandlede steg
                behandling.behandlingStegTilstand.filter { it.behandlingSteg.sekvens > behandledeSteg.sekvens }
                    .forEach { it.behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT }

                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                hentStegTilstandForBehandlingSteg(behandling, behandledeSteg).behandlingStegStatus =
                    BehandlingStegStatus.KLAR
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandledeSteg))

                utførSteg(behandlingId, behandledeSteg, behandlingStegDto)
            }

            BehandlingStegStatus.VENTER -> {
                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                hentStegTilstandForBehandlingSteg(behandling, behandledeSteg).behandlingStegStatus =
                    BehandlingStegStatus.KLAR
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandledeSteg))

                hentStegInstans(behandledeSteg).gjenopptaSteg(behandlingId)
            }
            // AVBRUTT kan brukes kun for henleggelse
            // TILBAKEFØRT steg blir oppdatert til KLAR når det forrige steget er behandlet
            BehandlingStegStatus.AVBRUTT, BehandlingStegStatus.TILBAKEFØRT ->
                throw Feil(
                    "Kan ikke behandle behandling $behandlingId " +
                        "med steg $behandledeSteg med status ${behandledeStegTilstand.behandlingStegStatus}"
                )
        }
    }

    private fun valider(behandling: Behandling, behandledeSteg: BehandlingSteg) {
        // valider om steget kan behandles
        if (!behandledeSteg.kanStegBehandles()) {
            throw Feil("Steget ${behandledeSteg.name} kan ikke behandles for behandling ${behandling.id}")
        }
        // valider om steget samsvarer med opprettet årsak til behandling
        behandledeSteg.gyldigForÅrsaker.singleOrNull { it == behandling.opprettetÅrsak }
            ?: throw Feil(
                "Steget ${behandledeSteg.name} er ikke gyldig for behandling ${behandling.id} " +
                    "med opprettetÅrsak ${behandling.opprettetÅrsak}"
            )

        // valider om et tidligere steg i behandlingen har stegstatus KLAR
        val stegKlarForBehandling = behandling.behandlingStegTilstand.singleOrNull {
            it.behandlingSteg.sekvens < behandledeSteg.sekvens &&
                it.behandlingStegStatus == BehandlingStegStatus.KLAR
        }
        if (stegKlarForBehandling != null) {
            throw Feil(
                "Behandling ${behandling.id} har allerede et steg " +
                    "${stegKlarForBehandling.behandlingSteg.name}} som er klar for behandling. " +
                    "Kan ikke behandle ${behandledeSteg.name}"
            )
        }

        // valider om inlogget bruker har riktig rolle
        // TODO tilgangssjekk
    }

    fun hentNesteSteg(behandling: Behandling, behandledeSteg: BehandlingSteg): BehandlingSteg {
        val nesteGyldigeStadier = BehandlingSteg.values().filter {
            it.sekvens > behandledeSteg.sekvens &&
                behandling.opprettetÅrsak in it.gyldigForÅrsaker
        }.sortedBy { it.sekvens }
        return when (behandledeSteg) {
            BehandlingSteg.BEHANDLING_AVSLUTTET -> throw Feil("Behandling ${behandling.id} er allerede avsluttet")
            BehandlingSteg.BESLUTTE_VEDTAK -> hentNesteStegEtterBeslutteVedtakBasertPåBehandlingsresultat(behandling.resultat)
            else -> nesteGyldigeStadier.first()
        }
    }

    fun settBehandlingPåVent(behandlingId: Long, frist: LocalDate, årsak: BehandlingSettPåVentÅrsak) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)
        validerBehandlingKanSettesPåVent(behandling, frist, årsak)

        loggService.opprettSettPåVentLogg(behandling, årsak.visningsnavn)
        logger.info("Setter behandling $behandlingId på vent med frist $frist og årsak $årsak")

        behandlingStegTilstand.frist = frist
        behandlingStegTilstand.årsak = årsak
        behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.VENTER
        behandlingRepository.saveAndFlush(behandling)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandling.id,
            forlengelse = Period.between(LocalDate.now(), frist)
        )
    }

    fun oppdaterFristOgEllerÅrsakPåVentendeBehandling(
        behandlingId: Long,
        frist: LocalDate,
        årsak: BehandlingSettPåVentÅrsak
    ) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)

        if (frist == behandlingStegTilstand.frist && årsak == behandlingStegTilstand.årsak) {
            throw FunksjonellFeil("Behandlingen er allerede satt på vent med frist $frist og årsak $årsak.")
        }

        loggService.opprettOppdaterVentingLogg(
            behandling = behandling,
            endretÅrsak = if (årsak != behandlingStegTilstand.årsak) årsak.visningsnavn else null,
            endretFrist = if (frist != behandlingStegTilstand.frist) frist else null
        )
        logger.info("Oppdater ventende behandling $behandlingId med frist $frist og årsak $årsak")

        val gammelFrist = behandlingStegTilstand.frist
        behandlingStegTilstand.frist = frist
        behandlingStegTilstand.årsak = årsak
        behandlingRepository.saveAndFlush(behandling)

        oppgaveService.forlengFristÅpneOppgaverPåBehandling(
            behandlingId = behandlingId,
            forlengelse = Period.between(gammelFrist, frist)
        )
    }

    fun gjenopptaBehandling(behandlingId: Long) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)

        loggService.opprettBehandlingGjenopptattLogg(behandling)
        logger.info("Gjenopptar behandling $behandlingId")

        behandlingStegTilstand.frist = null
        behandlingStegTilstand.årsak = null
        behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.KLAR
        behandlingRepository.saveAndFlush(behandling)

        oppgaveService.settFristÅpneOppgaverPåBehandlingTil(
            behandlingId = behandlingId,
            nyFrist = LocalDate.now().plusDays(1)
        )
    }

    fun validerBehandlingKanSettesPåVent(behandling: Behandling, frist: LocalDate, årsak: BehandlingSettPåVentÅrsak) {
        when {
            frist.isBefore(LocalDate.now()) -> {
                throw FunksjonellFeil(
                    melding = "Frist for å vente på behandling ${behandling.id} er satt før dagens dato.",
                    frontendFeilmelding = "Fristen er satt før dagens dato."
                )
            }

            behandling.status == BehandlingStatus.AVSLUTTET -> {
                throw FunksjonellFeil(
                    melding = "Behandling ${behandling.id} er avsluttet og kan ikke settes på vent.",
                    frontendFeilmelding = "Kan ikke sette en avsluttet behandling på vent."
                )
            }

            !behandling.aktiv -> {
                throw Feil(
                    "Behandling ${behandling.id} er ikke aktiv og kan ikke settes på vent."
                )
            }
        }
    }

    private fun hentNesteStegEtterBeslutteVedtakBasertPåBehandlingsresultat(resultat: Behandlingsresultat): BehandlingSteg {
        return when {
            resultat.kanIkkeSendesTilOppdrag() -> BehandlingSteg.JOURNALFØR_VEDTAKSBREV
            else -> BehandlingSteg.IVERKSETT_MOT_OPPDRAG
        }
    }

    private fun hentStegTilstandForBehandlingSteg(
        behandling: Behandling,
        behandledeSteg: BehandlingSteg
    ): BehandlingStegTilstand =
        behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandledeSteg }
            ?: throw Feil("$behandledeSteg finnes ikke i Behandling ${behandling.id}")

    private fun hentStegInstans(behandlingssteg: BehandlingSteg): IBehandlingSteg =
        steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
            ?: throw Feil("Finner ikke behandlingssteg $behandlingssteg")

    private fun oppdaterBehandlingStatus(behandling: Behandling, behandledeSteg: BehandlingSteg): Behandling {
        behandling.status = behandledeSteg.tilknyttetBehandlingStatus
        return behandling
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StegService::class.java)
    }
}
