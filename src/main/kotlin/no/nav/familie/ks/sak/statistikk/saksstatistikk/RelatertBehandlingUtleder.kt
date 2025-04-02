package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    @Lazy private val behandlingService: BehandlingService,
    private val unleashService: UnleashNextMedContextService,
) {
    private val logger: Logger = LoggerFactory.getLogger(RelatertBehandlingUtleder::class.java)

    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (!unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false)) {
            return null
        }

        if (behandling.erRevurderingKlage()) {
            return null // TODO : NAV-24658 implementer logikk for å hente relatert klagebehandling, trengs ikke før prodsetting
        }

        if (behandling.erRevurderingEllerTekniskEndring()) {
            val forrigeVedtatteKontantstøttebehandling = behandlingService.hentForrigeBehandlingSomErVedtatt(behandling)
            if (forrigeVedtatteKontantstøttebehandling == null) {
                logger.warn("Forventer en vedtatt kontantstøttebehandling for fagsak ${behandling.fagsak.id} og behandling ${behandling.id}")
                return null
            }
            return RelatertBehandling.fraKontantstøttebehandling(forrigeVedtatteKontantstøttebehandling)
        }

        return null
    }
}
