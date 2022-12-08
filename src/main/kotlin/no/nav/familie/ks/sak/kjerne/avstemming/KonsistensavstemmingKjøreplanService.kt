package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.ks.sak.kjerne.avstemming.domene.KjøreStatus
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingKjøreplan
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingKjøreplanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class KonsistensavstemmingKjøreplanService(
    private val konsistensavstemmingKjøreplanRepository: KonsistensavstemmingKjøreplanRepository
) {

    fun plukkLedigKjøreplanFor(dato: LocalDate): KonsistensavstemmingKjøreplan? {
        val kjøreplan = konsistensavstemmingKjøreplanRepository.findByKjøredatoAndLedig(dato)
        return kjøreplan?.also { lagreNyStatus(kjøreplan, KjøreStatus.BEHANDLER) }
    }

    fun lagreNyStatus(kjøreplan: KonsistensavstemmingKjøreplan, status: KjøreStatus) {
        kjøreplan.status = status
        konsistensavstemmingKjøreplanRepository.saveAndFlush(kjøreplan)
    }

    fun lagreNyStatus(kjøreplanId: Long, status: KjøreStatus) {
        val kjøreplan = konsistensavstemmingKjøreplanRepository.getReferenceById(kjøreplanId)
        kjøreplan.status = status
        konsistensavstemmingKjøreplanRepository.saveAndFlush(kjøreplan)
    }

    fun harKjøreplanStatusFerdig(kjøreplanId: Long): Boolean {
        val kjøreplan = konsistensavstemmingKjøreplanRepository.getReferenceById(kjøreplanId)
        return kjøreplan.status == KjøreStatus.FERDIG
    }

    @Transactional
    fun leggTilManuellKjøreplan(): KonsistensavstemmingKjøreplan {
        val kjøreplan = KonsistensavstemmingKjøreplan(kjøredato = LocalDate.now(), status = KjøreStatus.MANUELL)
        return konsistensavstemmingKjøreplanRepository.saveAndFlush(kjøreplan)
    }
}
