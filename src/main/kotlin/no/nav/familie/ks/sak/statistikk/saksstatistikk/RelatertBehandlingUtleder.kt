package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    @Lazy private val behandlingService: BehandlingService,
    @Lazy private val klageService: KlageService,
    private val unleashService: UnleashNextMedContextService,
) {
    private val logger: Logger = LoggerFactory.getLogger(RelatertBehandlingUtleder::class.java)

    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (!unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false)) {
            return null
        }

        if (behandling.erRevurderingKlage()) {
            val sisteVedtatteKlagebehandling = klageService.hentSisteVedtatteKlagebehandling(behandling.fagsak.id)
            if (sisteVedtatteKlagebehandling == null) {
                throw Feil("Forventer en vedtatt klagebehandling for fagsak ${behandling.fagsak.id} og behandling ${behandling.id}")
            }
            return RelatertBehandling.fraKlagebehandling(sisteVedtatteKlagebehandling)
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
