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
    fun hentAlleAdopsjonerForBehandling(behandlingId: BehandlingId): List<Adopsjon> = adopsjonRepository.hentAlleAdopsjonerForBehandling(behandlingId.id)

    fun finnAdopsjonForAktørIBehandling(
        aktør: Aktør,
        behandlingId: BehandlingId,
    ): Adopsjon? = adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId.id, aktør)

    @Transactional
    fun oppdaterAdopsjonsdato(
        behandlingId: BehandlingId,
        aktør: Aktør,
        nyAdopsjonsdato: LocalDate?,
    ) {
        val nåværendeAdopsjon = adopsjonRepository.finnAdopsjonForAktørIBehandling(behandlingId = behandlingId.id, aktør = aktør)

        if (nåværendeAdopsjon?.adopsjonsdato == nyAdopsjonsdato) {
            return
        }

        if (nåværendeAdopsjon != null) {
            adopsjonRepository.delete(nåværendeAdopsjon)
            adopsjonRepository.flush()
        }
        if (nyAdopsjonsdato != null) {
            adopsjonRepository.saveAndFlush(Adopsjon(behandlingId = behandlingId.id, aktør = aktør, adopsjonsdato = nyAdopsjonsdato))
        }
    }

    @Transactional
    fun kopierAdopsjonerFraForrigeBehandling(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId,
    ) {
        val adopsjonerForrigeBehandling = hentAlleAdopsjonerForBehandling(forrigeBehandlingId)

        adopsjonerForrigeBehandling.forEach { adopsjon ->
            val nyAdopsjon = Adopsjon(behandlingId = behandlingId.id, aktør = adopsjon.aktør, adopsjonsdato = adopsjon.adopsjonsdato)
            adopsjonRepository.save(nyAdopsjon)
        }
    }
}
