package no.nav.familie.ks.sak.kjerne.adopsjon

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AdopsjonService(
    private val adopsjonRepository: AdopsjonRepository,
) {
    fun hentAlleAdopsjonerForBehandling(behandlingId: Long): List<Adopsjon> = adopsjonRepository.hentAlleAdopsjonerForBehandling(behandlingId)

    @Transactional
    fun oppdaterAdopsjonsdato(behandlingId: Long, aktør: Aktør, nyAdopsjonsdato: LocalDate?) {
        val nåværendeAdopsjon = adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId = behandlingId, aktør = aktør)

        if (nåværendeAdopsjon?.adopsjonsdato != nyAdopsjonsdato) {
            if (nåværendeAdopsjon != null) {
                adopsjonRepository.delete(nåværendeAdopsjon)
                adopsjonRepository.flush()
            }
            if (nyAdopsjonsdato != null) {
                adopsjonRepository.saveAndFlush(Adopsjon(behandlingId = behandlingId, aktør = aktør, adopsjonsdato = nyAdopsjonsdato))
            }
        }
    }

    @Transactional
    fun kopierAdopsjonerFraForrigeBehandling(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId,
    ) {
        val adopsjonerForrigeBehandling = hentAlleAdopsjonerForBehandling(forrigeBehandlingId.id)

        adopsjonerForrigeBehandling.forEach { adopsjon ->
            val nyAdopsjon = Adopsjon(behandlingId = behandlingId.id, aktør = adopsjon.aktør, adopsjonsdato = adopsjon.adopsjonsdato)
            adopsjonRepository.save(nyAdopsjon)
        }
    }
}
