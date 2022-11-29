package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AvstemmingService(private val oppdragKlient: OppdragKlient) {

    fun sendGrensesnittavstemming(fom: LocalDateTime, tom: LocalDateTime) {
        oppdragKlient.sendGrensesnittavstemmingTilOppdrag(fom, tom)
    }
}
