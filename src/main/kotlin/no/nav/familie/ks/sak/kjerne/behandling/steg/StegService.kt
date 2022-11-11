package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StegService(
    private val steg: List<IBehandlingSteg>,
    private val behandlingRepository: BehandlingRepository,
    private val sakStatistikkService: SakStatistikkService
) {

    @Transactional
    fun utførSteg(behandlingId: Long, behandlingSteg: BehandlingSteg, behandlingStegDto: BehandlingStegDto? = null) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandlingSteg)

        valider(behandling, behandlingSteg)
        when (behandlingStegTilstand.behandlingStegStatus) {
            BehandlingStegStatus.KLAR -> {
                // utfør steg, kaller utfør metode i tilsvarende steg klasser
                behandlingStegDto?.let { hentStegInstans(behandlingSteg).utførSteg(behandlingId, it) }
                    ?: hentStegInstans(behandlingSteg).utførSteg(behandlingId)
                // oppdaterer nåværendeSteg status til utført
                behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.UTFØRT
                // Henter neste steg basert på sekvens og årsak
                val nesteSteg = hentNesteSteg(behandling, behandlingSteg)
                // legger til neste steg hvis steget er ny, eller oppdaterer eksisterende steg status til KLAR
                behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == nesteSteg }
                    ?.let { it.behandlingStegStatus = BehandlingStegStatus.KLAR }
                    ?: behandling.leggTilNesteSteg(nesteSteg)

                // oppdaterer behandling med behandlingstegtilstand og behandling status
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandlingSteg))
            }

            BehandlingStegStatus.UTFØRT -> {
                // tilbakefører alle stegene som er etter behandlede steg
                behandling.behandlingStegTilstand.filter { it.behandlingSteg.sekvens > behandlingSteg.sekvens }
                    .forEach { it.behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT }

                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                hentStegTilstandForBehandlingSteg(behandling, behandlingSteg).behandlingStegStatus =
                    BehandlingStegStatus.KLAR
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandlingSteg))

                utførSteg(behandlingId, behandlingSteg, behandlingStegDto)
            }

            BehandlingStegStatus.VENTER -> {
                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                logger.info("Gjenopptar behandling ${behandling.id}")

                behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.KLAR
                behandlingStegTilstand.frist = null
                behandlingStegTilstand.årsak = null
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandlingSteg))
            }
            // AVBRUTT kan brukes kun for henleggelse
            // TILBAKEFØRT steg blir oppdatert til KLAR når det forrige steget er behandlet
            BehandlingStegStatus.AVBRUTT, BehandlingStegStatus.TILBAKEFØRT ->
                throw Feil(
                    "Kan ikke behandle behandling $behandlingId " +
                        "med steg $behandlingSteg med status ${behandlingStegTilstand.behandlingStegStatus}"
                )
        }
        // statistikk til datavarehus
        sakStatistikkService.opprettSendingAvBehandlingensTilstand(behandlingId, behandlingSteg)
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

    @Transactional
    fun tilbakeførBehandlingSteg(behandling: Behandling, tilbakeføresSteg: BehandlingSteg): Behandling {
        val nåværendeBehandlingSteg = behandling.steg
        if (nåværendeBehandlingSteg.sekvens < tilbakeføresSteg.sekvens) {
            throw Feil(
                "Behandling ${behandling.id} er på ${nåværendeBehandlingSteg.visningsnavn()}, " +
                    "kan ikke tilbakeføres til ${tilbakeføresSteg.visningsnavn()}."
            )
        }
        behandling.behandlingStegTilstand.forEach {
            when (it.behandlingSteg) {
                nåværendeBehandlingSteg -> it.behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT
                tilbakeføresSteg -> it.behandlingStegStatus = BehandlingStegStatus.KLAR
                else -> {} // gjør ingenting
            }
        }
        return behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, tilbakeføresSteg))
    }

    fun settBehandlingstegPåVent(
        behandling: Behandling,
        frist: LocalDate
    ) {
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)

        logger.info("Setter behandling ${behandling.id} på vent med frist $frist og årsak ${VenteÅrsak.AVVENTER_DOKUMENTASJON}")

        behandlingStegTilstand.frist = frist
        behandlingStegTilstand.årsak = VenteÅrsak.AVVENTER_DOKUMENTASJON
        behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.VENTER
        behandlingRepository.saveAndFlush(behandling)
    }

    fun oppdaterBehandlingstegFrist(
        behandling: Behandling,
        frist: LocalDate
    ): LocalDate? {
        val behandlingStegTilstand = hentStegTilstandForBehandlingSteg(behandling, behandling.steg)

        if (frist == behandlingStegTilstand.frist) {
            throw FunksjonellFeil("Behandlingen er allerede satt på vent med frist $frist")
        }

        logger.info("Oppdater ventende behandling ${behandling.id} med frist $frist")

        val gammelFrist = behandlingStegTilstand.frist

        behandlingStegTilstand.frist = frist
        behandlingRepository.saveAndFlush(behandling)

        return gammelFrist
    }

    fun settAlleStegTilAvbrutt(behandling: Behandling) {
        behandling.behandlingStegTilstand.forEach { it.behandlingStegStatus = BehandlingStegStatus.AVBRUTT }
    }

    private fun hentNesteStegEtterBeslutteVedtakBasertPåBehandlingsresultat(resultat: Behandlingsresultat): BehandlingSteg {
        return when {
            resultat.kanIkkeSendesTilOppdrag() -> BehandlingSteg.JOURNALFØR_VEDTAKSBREV
            else -> BehandlingSteg.IVERKSETT_MOT_OPPDRAG
        }
    }

    private fun hentStegTilstandForBehandlingSteg(
        behandling: Behandling,
        behandlingSteg: BehandlingSteg
    ): BehandlingStegTilstand =
        behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandlingSteg }
            ?: throw Feil("$behandlingSteg finnes ikke i Behandling ${behandling.id}")

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
