package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.klage.KlagebehandlingHenter
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    private val behandlingRepository: BehandlingRepository,
    private val klagebehandlingHenter: KlagebehandlingHenter,
    private val unleashService: UnleashNextMedContextService,
) {
    fun utledRelatertBehandling(fagsakId: Long): RelatertBehandling? {
        if (!unleashService.isEnabled(FeatureToggle.KAN_BEHANDLE_KLAGE, false)) {
            return null
        }

        val forrigeKontantstøttebehandling =
            behandlingRepository
                .finnBehandlinger(fagsakId)
                .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
                .maxByOrNull { it.aktivertTidspunkt }
                ?.takeIf { harKontantstøttebehandlingKorrektBehandlingType(it) }
                ?.let { RelatertBehandling.fraKontantstøttebehandling(it) }

        val forrigeKlagebehandling =
            klagebehandlingHenter
                .hentSisteVedtatteKlagebehandling(fagsakId)
                ?.let { RelatertBehandling.fraKlagebehandling(it) }

        return listOfNotNull(forrigeKontantstøttebehandling, forrigeKlagebehandling).maxByOrNull { it.vedtattTidspunkt }
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
