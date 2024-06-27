package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService.Companion.oppdaterBehandlingStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakestillStegService(
    private val behandlingRepository: BehandlingRepository,
) {
    @Transactional
    fun tilbakeførSteg(
        behandlingId: Long,
        behandlingSteg: BehandlingSteg,
    ) {
        val behandling = behandlingRepository.hentAktivBehandling(behandlingId)
        val behandlingStegTilstand = behandling.behandlingStegTilstand.single { it.behandlingSteg == behandlingSteg }

        val erAlleredeTilbakeført = behandlingStegTilstand.behandlingStegStatus == BehandlingStegStatus.KLAR
        if (erAlleredeTilbakeført) {
            return
        }

        settAlleEtterfølgendeStegTilStatusTilbakeført(behandling, behandlingSteg)

        behandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.KLAR
        behandlingRepository.saveAndFlush(oppdaterBehandlingStatus(behandling))
    }

    companion object {
        private fun settAlleEtterfølgendeStegTilStatusTilbakeført(
            behandling: Behandling,
            behandlingSteg: BehandlingSteg,
        ) {
            behandling.behandlingStegTilstand
                .filter { it.behandlingSteg.sekvens > behandlingSteg.sekvens }
                .forEach { it.behandlingStegStatus = BehandlingStegStatus.TILBAKEFØRT }
        }
    }
}
