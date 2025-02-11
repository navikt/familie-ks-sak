package no.nav.familie.ks.sak.kjerne.adopsjon

import org.springframework.stereotype.Service

@Service
class AdopsjonService(
    private val adopsjonRepository: AdopsjonRepository,
) {
    fun hentAlleAdopsjonerForBehandling(behandlingId: Long): List<Adopsjon> = adopsjonRepository.hentAlleAdopsjonerForBehandling(behandlingId)
}
