package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SnikeIKøenService(
    private val behandlingService: BehandlingService,
    private val loggService: LoggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settAktivBehandlingPåMaskinellVent(
        behandlingId: Long,
        årsak: SettPåMaskinellVentÅrsak,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (!behandling.aktiv) {
            error("Behandling=$behandlingId er ikke aktiv")
        }
        val behandlingStatus = behandling.status
        val behandlingStegTilstand = finnBehandlingStegTilstand(behandling)

        val erBehanlingPåVent = behandlingStegTilstand.behandlingStegStatus === BehandlingStegStatus.VENTER

        if (behandlingStatus !== BehandlingStatus.UTREDES && !erBehanlingPåVent) {
            error("Behandling=$behandlingId kan ikke settes på maskinell vent då status=$behandlingStatus")
        }

        behandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandling.aktiv = false
        behandlingService.oppdaterBehandling(behandling)
        loggService.opprettSettPåMaskinellVent(behandling, årsak.årsak)
    }

    private fun finnBehandlingStegTilstand(behandling: Behandling) =
        behandling.behandlingStegTilstand.single { it.behandlingSteg == behandling.steg }

    /**
     * @param behandlingSomFerdigstilles er behandlingen som ferdigstilles i [no.nav.familie.ba.sak.kjerne.steg.FerdigstillBehandling]
     *  Den er mest brukt for å logge hvilken behandling det er som ferdigstilles og hvilken som blir deaktivert
     *
     * @return boolean som tilsier om en behandling er reaktivert eller ikke
     */
    @Transactional
    fun reaktiverBehandlingPåMaskinellVent(behandlingSomFerdigstilles: Behandling): Boolean {
        val fagsakId = behandlingSomFerdigstilles.fagsak.id

        val behandlingPåMaskinellVent = finnBehandlingPåMaskinellVent(fagsakId) ?: return false

        val behandlingStegTilstand = finnBehandlingStegTilstand(behandlingPåMaskinellVent)
        val erBehandlingStegStatusVenter = behandlingStegTilstand.behandlingStegStatus == BehandlingStegStatus.VENTER

        val aktivBehandling = behandlingService.hentBehandlingerPåFagsak(fagsakId).singleOrNull { it.aktiv }

        validerBehandlinger(aktivBehandling, behandlingPåMaskinellVent)

        aktiverBehandlingPåVent(aktivBehandling, behandlingPåMaskinellVent, behandlingSomFerdigstilles)

        tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåMaskinellVent.id)
        if (erBehandlingStegStatusVenter) {
            val behandlingStegTilstandEtterTilbakestilling = finnBehandlingStegTilstand(behandlingPåMaskinellVent)
            behandlingStegTilstandEtterTilbakestilling.behandlingStegStatus = BehandlingStegStatus.VENTER
            behandlingStegTilstandEtterTilbakestilling.frist = behandlingStegTilstand.frist
            behandlingStegTilstandEtterTilbakestilling.årsak = behandlingStegTilstand.årsak
        }

        loggService.opprettTattAvMaskinellVent(behandlingPåMaskinellVent)
        return true
    }

    fun kanSnikeForbi(aktivOgÅpenBehandling: Behandling): Boolean {
        val behandlingId = aktivOgÅpenBehandling.id
        val loggSuffix = "endrer status på behandling til på vent"

        val behandlingStegStatus = finnBehandlingStegTilstand(aktivOgÅpenBehandling).behandlingStegStatus
        if (behandlingStegStatus == BehandlingStegStatus.VENTER) {
            logger.info("Behandling=$behandlingId er satt på vent av saksbehandler, $loggSuffix")
            return true
        }
        val tid4TimerSiden = LocalDateTime.now().minusHours(4)
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

    private fun finnBehandlingPåMaskinellVent(
        fagsakId: Long,
    ): Behandling? =
        behandlingService.hentBehandlingerPåFagsak(fagsakId).filter {
            it.status == BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        }
            .takeIf { it.isNotEmpty() }
            ?.let { it.singleOrNull() ?: error("Forventer kun en behandling på vent for fagsak=$fagsakId") }

    private fun aktiverBehandlingPåVent(
        aktivBehandling: Behandling?,
        behandlingPåVent: Behandling,
        behandlingSomFerdigstilles: Behandling,
    ) {
        logger.info(
            "Deaktiverer aktivBehandling=${aktivBehandling?.id}" +
                " aktiverer behandlingPåVent=${behandlingPåVent.id}" +
                " behandlingSomFerdigstilles=${behandlingSomFerdigstilles.id}",
        )

        if (aktivBehandling != null) {
            aktivBehandling.aktiv = false
            behandlingService.oppdaterBehandling(aktivBehandling)
        }

        behandlingPåVent.aktiv = true
        behandlingPåVent.aktivertTidspunkt = LocalDateTime.now()
        behandlingPåVent.status = BehandlingStatus.UTREDES

        behandlingService.oppdaterBehandling(behandlingPåVent)
    }

    private fun validerBehandlinger(
        aktivBehandling: Behandling?,
        behandlingPåVent: Behandling,
    ) {
        if (behandlingPåVent.aktiv) {
            error("Åpen behandling har feil tilstand $behandlingPåVent")
        }
        if (aktivBehandling != null && aktivBehandling.status != BehandlingStatus.AVSLUTTET) {
            throw BehandlingErIkkeAvsluttetException(aktivBehandling)
        }
    }
}

private fun Behandling.harVærtPåVilkårsvurderingSteg() =
    behandlingStegTilstand.any { it.behandlingSteg == BehandlingSteg.VILKÅRSVURDERING }

enum class SettPåMaskinellVentÅrsak(val årsak: String) {
    SATSENDRING("Satsendring"),
    MÅNEDLIG_VALUTAJUSTERING("Månedlig valutajustering"),
}

class BehandlingErIkkeAvsluttetException(val behandling: Behandling) :
    RuntimeException("Behandling=${behandling.id} har status=${behandling.status} og er ikke avsluttet")
