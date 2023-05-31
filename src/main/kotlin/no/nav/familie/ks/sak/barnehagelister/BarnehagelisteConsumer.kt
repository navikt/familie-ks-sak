package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.ks.sak.barnehagelister.domene.BarnehagelisteMottatt
import no.nav.familie.ks.sak.config.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod")
class BarnehagelisteConsumer(val barnehageListeService: BarnehageListeService) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-ks-sak",
        topics = [KafkaConfig.BARNEHAGELISTE_TOPIC],
        containerFactory = "concurrentKafkaListenerContainerFactory",
    )
    fun listen(consumerRecord: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val data: String = consumerRecord.value()
        val key: String = consumerRecord.key()

        logger.info("Barnehageliste mottatt på kafka med key $key")

        // Sjekk at vi ikke har mottat meldingen tidligere
        if (barnehageListeService.erListenMottattTidligere(key)) {
            logger.info("Barnehageliste er med key $key er mottatt tidligere hopper over")
            ack.acknowledge()
            return
        }

        barnehageListeService.lagreBarnehageliste(
            BarnehagelisteMottatt(
                meldingId = key,
                melding = data,
                mottat_tid = LocalDateTime.now(),
            ),
        )
        ack.acknowledge()
    }
}
