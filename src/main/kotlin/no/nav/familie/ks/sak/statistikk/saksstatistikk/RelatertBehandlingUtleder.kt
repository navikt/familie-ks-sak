package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.klage.KlageService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    @Lazy private val behandlingService: BehandlingService,
    @Lazy private val klageService: KlageService,
    private val unleashService: UnleashNextMedContextService,
) {
    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (!unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false)) {
            return null
        }
        return if (behandling.erRevurderingKlage()) {
            val sisteVedtatteKlagebehandling = klageService.hentSisteVedtatteKlagebehandling(behandling.fagsak.id)
            if (sisteVedtatteKlagebehandling == null) {
                throw Feil("Forventer en vedtatt klagebehandling for behandling ${behandling.behandlingId}")
            }
            RelatertBehandling.fraKlagebehandling(sisteVedtatteKlagebehandling)
        } else {
            behandlingService
                .hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
                ?.takeIf { harKontantstøttebehandlingKorrektBehandlingType(it) }
                ?.let { RelatertBehandling.fraKontantstøttebehandling(it) }
        }
    }

    private fun harKontantstøttebehandlingKorrektBehandlingType(kontantstøttebehandling: Behandling) =
        when (kontantstøttebehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING,
            -> false

            BehandlingType.REVURDERING,
            BehandlingType.TEKNISK_ENDRING,
            -> true
        }
}
