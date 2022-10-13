package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import org.springframework.stereotype.Service

@Service
class BehandlingHentService(private val behandlingRepository: BehandlingRepository) {

    fun hentSisteBehandlingSomErVedtatt(fagsakId: Long): Behandling? = behandlingRepository.finnBehandlinger(fagsakId)
        .filter { !it.erHenlagt() && it.status == BehandlingStatus.AVSLUTTET }
        .maxByOrNull { it.opprettetTidspunkt }

    fun hentBehandling(behandlingId: Long): Behandling = behandlingRepository.hentBehandling(behandlingId)
}
