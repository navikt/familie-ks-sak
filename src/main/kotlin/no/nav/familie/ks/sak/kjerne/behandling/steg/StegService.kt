package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(
    private val steg: List<IBehandlingSteg>,
    private val behandlingRepository: BehandlingRepository
) {

    @Transactional
    fun utførSteg(behandlingId: Long, behandledeSteg: BehandlingSteg) {
        val behandling = behandlingRepository.finnAktivBehandling(behandlingId)
        val behandledeStegTilstand = hentBehandledeSteg(behandling, behandledeSteg)

        valider(behandling, behandledeSteg)

        when (behandledeStegTilstand.behandlingStegStatus) {
            BehandlingStegStatus.KLAR -> {
                // utfør steg, kaller utfør metode i tilsvarende steg klasser
                hentStegInstans(behandledeSteg).utførSteg(behandlingId)
                // oppdaterer nåværendeSteg status til utført
                hentBehandledeSteg(behandling, behandledeSteg).behandlingStegStatus = BehandlingStegStatus.UTFØRT
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
                hentBehandledeSteg(behandling, behandledeSteg).behandlingStegStatus = BehandlingStegStatus.KLAR
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandledeSteg))

                utførSteg(behandlingId, behandledeSteg)
            }
            BehandlingStegStatus.VENTER -> {
                // oppdaterte behandling med behandlede steg som KLAR slik at det kan behandles
                hentBehandledeSteg(behandling, behandledeSteg).behandlingStegStatus = BehandlingStegStatus.KLAR
                behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling, behandledeSteg))

                hentStegInstans(behandledeSteg).gjenopptaSteg(behandlingId)
            }
            // AVBRUTT kan brukes kun for henleggelse
            // TILBAKEFØRT steg blir oppdatert til KLAR når det forrige steget er behandlet
            BehandlingStegStatus.AVBRUTT, BehandlingStegStatus.TILBAKEFØRT ->
                throw Feil(
                    "Kan ikke behandle behandling $behandlingId " +
                        "med steg $behandledeSteg med status ${BehandlingStegStatus.AVBRUTT}"
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

    private fun hentNesteStegEtterBeslutteVedtakBasertPåBehandlingsresultat(resultat: Behandlingsresultat): BehandlingSteg {
        return when {
            resultat.kanIkkeSendesTilOppdrag() -> BehandlingSteg.JOURNALFØR_VEDTAKSBREV
            else -> BehandlingSteg.IVERKSETT_MOT_OPPDRAG
        }
    }

    private fun hentBehandledeSteg(behandling: Behandling, behandledeSteg: BehandlingSteg): BehandlingStegTilstand =
        behandling.behandlingStegTilstand.singleOrNull { it.behandlingSteg == behandledeSteg }
            ?: throw Feil("$behandledeSteg finnes ikke i Behandling ${behandling.id}")

    private fun hentStegInstans(behandlingssteg: BehandlingSteg): IBehandlingSteg =
        steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
            ?: throw Feil("Finner ikke behandlingssteg $behandlingssteg")

    private fun oppdaterBehandlingStatus(behandling: Behandling, behandledeSteg: BehandlingSteg): Behandling {
        behandling.status = behandledeSteg.tilknyttetBehandlingStatus
        return behandling
    }
}
