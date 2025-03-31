package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.barnehagelister.domene.KSBarnehagebarnDTO
import no.nav.familie.ks.sak.config.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod & !postgres")
class KSBarnehagelisterConsumer(
    val barnehageBarnService: BarnehagebarnService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(
        id = "familie-ks-barnehagelister",
        groupId = "familie-ks-barnehagelister-group",
        topics = [KafkaConfig.BARNEHAGELISTE_TOPIC],
        containerFactory = "earliestConcurrentKafkaListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        logger.info("Barnehagebarn mottatt fra familie-ks-barnehagelister med id ${consumerRecord.key()}")

        val barnehagebarn: Barnehagebarn =
            objectMapper
                .readValue(consumerRecord.value(), KSBarnehagebarnDTO::class.java)
                .tilBarnehagebarn()

        // Sjekk at vi ikke har mottat meldingen tidligere
        if (barnehageBarnService.erBarnehageBarnMottattTidligere(barnehagebarn)) {
            logger.info("Barnehagebarn med id ${consumerRecord.key()} er mottatt tidligere. Hopper over")
            ack.acknowledge()
            return
        }

        barnehageBarnService.lagreBarnehageBarn(barnehagebarn)

        ack.acknowledge()
    }
}
