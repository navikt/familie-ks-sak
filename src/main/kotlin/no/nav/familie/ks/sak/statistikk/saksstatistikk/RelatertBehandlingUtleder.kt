package no.nav.familie.ks.sak.statistikk.saksstatistikk

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
) {
    fun utledRelatertBehandling(fagsakId: Long): RelatertBehandling? {
        val forrigeKontantstøttebehandling =
            behandlingRepository // TODO : Bytt ut med BehandlingService når circular dependency er fikset
                .finnBehandlinger(fagsakId)
                .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
                .maxByOrNull { it.aktivertTidspunkt }
                ?.takeIf { harKontantstøttebehandlingKorrektBehandlingType(it) }
                ?.let { RelatertBehandling.fraKontantstøttebehandling(it) }

        val forrigeKlagebehandling =
            klagebehandlingHenter // TODO : Bytt ut med KlageService når circular dependency er fikset
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
