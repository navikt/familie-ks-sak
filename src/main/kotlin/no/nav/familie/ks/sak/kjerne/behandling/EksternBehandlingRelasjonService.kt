package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternBehandlingRelasjonService(
    private val eksternBehandlingRelasjonRepository: EksternBehandlingRelasjonRepository,
) {
    @Transactional
    fun lagreEksternBehandlingRelasjon(eksternBehandlingRelasjon: EksternBehandlingRelasjon): EksternBehandlingRelasjon = eksternBehandlingRelasjonRepository.save(eksternBehandlingRelasjon)

    fun finnEksternBehandlingRelasjon(
        behandlingId: Long,
        fagsystem: EksternBehandlingRelasjon.Fagsystem,
    ): EksternBehandlingRelasjon? = eksternBehandlingRelasjonRepository.findByInternBehandlingIdOgFagsystem(behandlingId, fagsystem)
}
