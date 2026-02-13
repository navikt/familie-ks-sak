package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.log.mdc.kjørMedCallId
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod & !postgres")
class HentFagsystemsbehandlingRequestConsumer(
    private val fagsystemsbehandlingService: FagsystemsbehandlingService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @KafkaListener(
        id = "familie-ks-sak",
        topics = ["\${TILBAKEKREVING_REQUEST_TOPIC}"],
        containerFactory = "concurrentKafkaListenerContainerFactory",
    )
    fun listen(
        consumerRecord: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val key: String = consumerRecord.key()
        kjørMedCallId(key) {
            val data: String = consumerRecord.value()
            val request = jsonMapper.readValue(data, HentFagsystemsbehandlingRequest::class.java)

            if (request.ytelsestype == Ytelsestype.KONTANTSTØTTE) {
                logger.info("HentFagsystemsbehandlingRequest er mottatt i kafka $consumerRecord med key $key")
                secureLogger.info("HentFagsystemsbehandlingRequest er mottatt i kafka $consumerRecord med key $key")

                val fagsystemsbehandling =
                    try {
                        fagsystemsbehandlingService.hentFagsystemsbehandling(request)
                    } catch (e: Exception) {
                        logger.warn(
                            "Noe gikk galt mens sender HentFagsystemsbehandlingRespons for behandling=${request.eksternId}. " +
                                "Feiler med ${e.message}",
                        )
                        HentFagsystemsbehandlingRespons(feilMelding = e.message)
                    }
                // Sender respons via kafka
                fagsystemsbehandlingService.sendFagsystemsbehandlingRespons(fagsystemsbehandling, key, request.eksternId)
            }

            ack.acknowledge()
        }
    }
}
