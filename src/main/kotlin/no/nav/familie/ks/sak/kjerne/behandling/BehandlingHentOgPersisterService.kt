package no.nav.familie.ks.sak.kjerne.behandling

import org.springframework.stereotype.Service

@Service
class BehandlingHentOgPersisterService(private val behandlingRepository: BehandlingRepository) {

    fun hent(behandlingId: Long): Behandling = behandlingRepository.finnBehandling(behandlingId)
    fun hentBehandlinger(fagsakId: Long): List<Behandling> = behandlingRepository.finnBehandlinger(fagsakId)
}
