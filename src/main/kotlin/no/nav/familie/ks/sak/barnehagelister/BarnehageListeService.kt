package no.nav.familie.ks.sak.barnehagelister

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottattRepository
import org.springframework.stereotype.Service

@Service
class BarnehageListeService(val barnehagelisteMottattRepository: BarnehagelisteMottattRepository) {

    @Transactional
    fun lagreBarnehageliste(barnehagelisteMottatt: BarnehagelisteMottatt) {
        barnehagelisteMottattRepository.save(barnehagelisteMottatt)
    }

    fun erListenMottattTidligere(meldingId: String): Boolean {
        return barnehagelisteMottattRepository.existsByMeldingId(meldingId)
    }
}
