package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class SnikeIKøenService(
    private val behandlingService: BehandlingService,
    private val loggService: LoggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settAktivBehandlingPåMaskinellVent(
        behandlingId: Long,
        årsak: SettPåMaskinellVentÅrsak,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        Validator.validerBehandlingSomSkalSettesPåMaskinellVent(behandling)
        behandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandling.aktiv = false
        behandlingService.oppdaterBehandling(behandling)
        loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse)
    }

    /**
     * @param behandlingSomAvsluttes er behandlingen som ferdigstilles i [no.nav.familie.ba.sak.kjerne.steg.FerdigstillBehandling]
     *  Den er mest brukt for å logge hvilken behandling det er som ferdigstilles og hvilken som blir deaktivert
     *
     * @return reaktivert enum som tilsier om en behandling er reaktivert eller ikke.
     */
    @Transactional
    fun reaktiverBehandlingPåMaskinellVent(behandlingSomAvsluttes: Behandling): Reaktivert {
        val behandlingerPåFagsak = behandlingService.hentBehandlingerPåFagsak(behandlingSomAvsluttes.fagsak.id)
        val behandlingPåMaskinellVent = finnBehandlingPåMaskinellVent(behandlingerPåFagsak) ?: return Reaktivert.NEI
        val behandlingStegTilstand = finnBehandlingStegTilstand(behandlingPåMaskinellVent)
        val erBehandlingStegStatusVenter = behandlingStegTilstand.behandlingStegStatus == BehandlingStegStatus.VENTER
        val aktivBehandling = behandlingerPåFagsak.singleOrNull { it.aktiv }
        Validator.validerBehandlingPåMaskinellVentFørReaktivering(behandlingPåMaskinellVent)
        Validator.validerAktivBehandlingFørReaktivering(aktivBehandling)
        deaktiverAktivBehandling(aktivBehandling)
        aktiverBehandlingPåMaskinellVent(behandlingPåMaskinellVent)
        tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåMaskinellVent.id)
        if (erBehandlingStegStatusVenter) {
            val behandlingStegTilstandEtterTilbakestilling = finnBehandlingStegTilstand(behandlingPåMaskinellVent)
            behandlingStegTilstandEtterTilbakestilling.behandlingStegStatus = BehandlingStegStatus.VENTER
            behandlingStegTilstandEtterTilbakestilling.frist = behandlingStegTilstand.frist
            behandlingStegTilstandEtterTilbakestilling.årsak = behandlingStegTilstand.årsak
        }
        loggService.opprettTattAvMaskinellVent(behandlingPåMaskinellVent)
        return Reaktivert.JA
    }

    fun kanSnikeForbi(aktivOgÅpenBehandling: Behandling): Boolean {
        val behandlingId = aktivOgÅpenBehandling.id
        val loggSuffix = "endrer status på behandling til på vent"
        val behandlingStegStatus = Companion.finnBehandlingStegTilstand(aktivOgÅpenBehandling).behandlingStegStatus
        if (behandlingStegStatus == BehandlingStegStatus.VENTER) {
            logger.info("Behandling=$behandlingId er satt på vent av saksbehandler, $loggSuffix")
            return true
        }
        val tid4TimerSiden = LocalDateTime.now(clock).minusHours(4)
        if (aktivOgÅpenBehandling.endretTidspunkt.isAfter(tid4TimerSiden)) {
            logger.info(
                "Behandling=$behandlingId har endretTid=${aktivOgÅpenBehandling.endretTidspunkt}. " +
                    "Det er altså mindre enn 4 timer siden behandlingen var endret, og vi ønsker derfor ikke å sette behandlingen på maskinell vent",
            )
            return false
        }
        val sisteLogghendelse = loggService.hentLoggForBehandling(behandlingId).maxBy { it.opprettetTidspunkt }
        if (sisteLogghendelse.opprettetTidspunkt.isAfter(tid4TimerSiden)) {
            logger.info(
                "Behandling=$behandlingId siste logginslag er " +
                    "type=${sisteLogghendelse.type} tid=${sisteLogghendelse.opprettetTidspunkt}, $loggSuffix. " +
                    "Det er altså mindre enn 4 timer siden siste logginslag, og vi ønsker derfor ikke å sette behandlingen på maskinell vent",
            )
            return false
        }
        return true
    }

    private fun deaktiverAktivBehandling(aktivBehandling: Behandling?) {
        if (aktivBehandling != null) {
            logger.info("Deaktiverer aktiv behandling=${aktivBehandling.id}")
            aktivBehandling.aktiv = false
            behandlingService.oppdaterBehandling(aktivBehandling)
        } else {
            logger.info("Fant ingen aktiv behandling å deaktivere")
        }
    }

    private fun aktiverBehandlingPåMaskinellVent(behandlingPåMaskinellVent: Behandling) {
        logger.info("Aktiverer behandling=${behandlingPåMaskinellVent.id} som er på maskinell vent")
        behandlingPåMaskinellVent.aktiv = true
        behandlingPåMaskinellVent.aktivertTidspunkt = LocalDateTime.now()
        behandlingPåMaskinellVent.status = BehandlingStatus.UTREDES
        behandlingService.oppdaterBehandling(behandlingPåMaskinellVent)
    }

    companion object {
        private fun finnBehandlingStegTilstand(behandling: Behandling) =
            behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        private fun finnBehandlingPåMaskinellVent(behandlingerPåFagsak: List<Behandling>): Behandling? {
            val behandlingerPåMaskinellVent = behandlingerPåFagsak.filter { it.status == BehandlingStatus.SATT_PÅ_MASKINELL_VENT }
            if (behandlingerPåMaskinellVent.isEmpty()) {
                return null
            }
            return behandlingerPåMaskinellVent.singleOrNull() ?: throw IllegalStateException(
                "Forventer kun en behandling på maskinell vent for fagsak=${behandlingerPåFagsak.first().fagsak.id}",
            )
        }
    }

    private class Validator {
        companion object {
            fun validerBehandlingSomSkalSettesPåMaskinellVent(behandling: Behandling) {
                if (!behandling.aktiv) {
                    throw IllegalStateException("Behandling=${behandling.id} er ikke aktiv")
                }
                val behandlingStatus = behandling.status
                val behandlingStegTilstand = finnBehandlingStegTilstand(behandling)
                val erBehandlingIkkePåVent = behandlingStegTilstand.behandlingStegStatus !== BehandlingStegStatus.VENTER
                if (erBehandlingIkkePåVent && behandlingStatus !== BehandlingStatus.UTREDES) {
                    throw IllegalStateException("Behandling=${behandling.id} kan ikke settes på maskinell vent da status=$behandlingStatus")
                }
            }

            fun validerAktivBehandlingFørReaktivering(aktivBehandling: Behandling?) {
                if (aktivBehandling != null && aktivBehandling.status != BehandlingStatus.AVSLUTTET) {
                    throw IllegalStateException(
                        "Behandling=${aktivBehandling.id} har status=${aktivBehandling.status} og er ikke avsluttet",
                    )
                }
            }

            fun validerBehandlingPåMaskinellVentFørReaktivering(behandlingPåMaskinellVent: Behandling) {
                if (behandlingPåMaskinellVent.aktiv) {
                    throw IllegalStateException("Behandling på maskinell vent er aktiv")
                }
            }
        }
    }
}

enum class SettPåMaskinellVentÅrsak(val beskrivelse: String) {
    SATSENDRING("Satsendring"),
    MÅNEDLIG_VALUTAJUSTERING("Månedlig valutajustering"),
}

enum class Reaktivert {
    JA,
    NEI,
}
