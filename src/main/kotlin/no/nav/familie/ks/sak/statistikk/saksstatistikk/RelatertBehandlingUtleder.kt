package no.nav.familie.ks.sak.statistikk.saksstatistikk

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
    fun utledRelatertBehandling(fagsakId: Long): RelatertBehandling? {
        if (!unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false)) {
            return null
        }

        val sisteVedtatteKontantstøttebehandling =
            behandlingService
                .hentSisteBehandlingSomErVedtatt(fagsakId)
                ?.takeIf { harKontantstøttebehandlingKorrektBehandlingType(it) }
                ?.let { RelatertBehandling.fraKontantstøttebehandling(it) }

        val sisteVedtatteKlagebehandling =
            klageService
                .hentSisteVedtatteKlagebehandling(fagsakId)
                ?.let { RelatertBehandling.fraKlagebehandling(it) }

        return listOfNotNull(sisteVedtatteKontantstøttebehandling, sisteVedtatteKlagebehandling).maxByOrNull { it.vedtattTidspunkt }
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
