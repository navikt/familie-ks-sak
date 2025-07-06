package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.common.ClockProvider
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SnikeIKøenService(
    private val behandlingRepository: BehandlingRepository,
    private val loggService: LoggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val clockProvider: ClockProvider,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settAktivBehandlingPåMaskinellVent(
        behandlingId: Long,
        årsak: SettPåMaskinellVentÅrsak,
    ) {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        Validator.validerBehandlingSomSkalSettesPåMaskinellVent(behandling)
        behandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandling.aktiv = false
        behandlingRepository.saveAndFlush(behandling)
        loggService.opprettSettPåMaskinellVent(behandling, årsak.beskrivelse)
    }

    @Transactional
    fun reaktiverBehandlingPåMaskinellVent(behandlingSomAvsluttes: Behandling): Reaktivert {
        val behandlingerPåFagsak = behandlingRepository.finnBehandlinger(behandlingSomAvsluttes.fagsak.id)
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
        val behandlingStegStatus = finnBehandlingStegTilstand(aktivOgÅpenBehandling).behandlingStegStatus
        if (behandlingStegStatus == BehandlingStegStatus.VENTER) {
            logger.info("Behandling=$behandlingId er satt på vent av saksbehandler, $loggSuffix")
            return true
        }
        val tid4TimerSiden = LocalDateTime.now(clockProvider.get()).minusHours(4)
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
            behandlingRepository.saveAndFlush(aktivBehandling)
        } else {
            logger.info("Fant ingen aktiv behandling å deaktivere")
        }
    }

    private fun aktiverBehandlingPåMaskinellVent(behandlingPåMaskinellVent: Behandling) {
        logger.info("Aktiverer behandling=${behandlingPåMaskinellVent.id} som er på maskinell vent")
        behandlingPåMaskinellVent.aktiv = true
        behandlingPåMaskinellVent.aktivertTidspunkt = LocalDateTime.now()
        behandlingPåMaskinellVent.status = BehandlingStatus.UTREDES
        behandlingRepository.saveAndFlush(behandlingPåMaskinellVent)
    }

    companion object {
        private fun finnBehandlingStegTilstand(behandling: Behandling) = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

        private fun finnBehandlingPåMaskinellVent(behandlingerPåFagsak: List<Behandling>): Behandling? {
            val behandlingerPåMaskinellVent = behandlingerPåFagsak.filter { it.status == BehandlingStatus.SATT_PÅ_MASKINELL_VENT }
            Validator.validerAntallBehandlingerPåMaskinellVent(behandlingerPåMaskinellVent)
            return behandlingerPåMaskinellVent.singleOrNull()
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

            fun validerAntallBehandlingerPåMaskinellVent(behandlingerPåMaskinellVent: List<Behandling>) {
                if (behandlingerPåMaskinellVent.size > 1) {
                    throw IllegalStateException(
                        "Forventet kun en eller ingen behandling på maskinell vent for fagsak=${behandlingerPåMaskinellVent.first().fagsak.id}",
                    )
                }
            }
        }
    }
}

enum class SettPåMaskinellVentÅrsak(
    val beskrivelse: String,
) {
    LOVENDRING("Lovendring"),
    SATSENDRING("Satsendring"),
    MÅNEDLIG_VALUTAJUSTERING("Månedlig valutajustering"),
}

enum class Reaktivert {
    JA,
    NEI,
}
