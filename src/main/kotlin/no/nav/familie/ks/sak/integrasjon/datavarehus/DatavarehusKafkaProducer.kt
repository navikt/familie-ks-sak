package no.nav.familie.ks.sak.integrasjon.datavarehus

import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.KafkaConfig
import no.nav.familie.ks.sak.config.KafkaConfig.Companion.VEDTAK_TOPIC
import no.nav.familie.ks.sak.statistikk.saksstatistikk.BehandlingStatistikkDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

interface KafkaProducer {
    fun sendBehandlingsTilstand(behandlingId: String, request: BehandlingStatistikkDto)
    fun sendSisteBehandlingsTilstand(request: BehandlingStatistikkDto)

    fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long
}

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod")
class DatavarehusKafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) : KafkaProducer {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun sendBehandlingsTilstand(behandlingId: String, request: BehandlingStatistikkDto) {
        sendKafkamelding(behandlingId, KafkaConfig.BEHANDLING_TOPIC, request.behandlingID.toString(), request)
    }

    override fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long {
        val vedtakForDVHV2Melding = objectMapper.writeValueAsString(vedtakV2)
        val response = kafkaTemplate.send(VEDTAK_TOPIC, vedtakV2.funksjonellId, vedtakForDVHV2Melding).get()

        return response.recordMetadata.offset()
    }

    override fun sendSisteBehandlingsTilstand(request: BehandlingStatistikkDto) {
        sendKafkamelding(
            request.behandlingID.toString(),
            KafkaConfig.SISTE_TILSTAND_BEHANDLING_TOPIC,
            request.behandlingID.toString(),
            request
        )
    }

    private fun sendKafkamelding(behandlingId: String, topic: String, key: String, request: Any) {
        val melding = objectMapper.writeValueAsString(request)
        val producerRecord = ProducerRecord(topic, key, melding)
        kafkaTemplate.send(producerRecord)
            .addCallback(
                {
                    log.info(
                        "Melding på topic $topic for $behandlingId med $key er sendt. " +
                            "Fikk offset ${it?.recordMetadata?.offset()}"
                    )
                },
                {
                    val feilmelding = "Melding på topic $topic kan ikke sendes for $behandlingId med $key. " +
                        "Feiler med ${it.message}"
                    log.warn(feilmelding)
                    throw Feil(message = feilmelding)
                }
            )
    }
}

@Service
@Profile("e2e", "integrasjonstest", "dev-postgres-preprod")
class E2EKafkaProducer : KafkaProducer {

    override fun sendBehandlingsTilstand(behandlingId: String, request: BehandlingStatistikkDto) {
        logger.info("Skipper sending av saksstatistikk for behandling $behandlingId fordi kafka ikke er enablet")
    }

    override fun sendSisteBehandlingsTilstand(request: BehandlingStatistikkDto) {
        logger.info("Skipper sending av saksstatistikk for behandling ${request.behandlingID} fordi kafka ikke er enablet")
    }

    override fun sendMessageForTopicVedtakV2(vedtakV2: VedtakDVHV2): Long {
        // TODO: Undersøke framtiden til E2EKafkaProducer
        return 0
    }

    companion object {

        private val logger = LoggerFactory.getLogger(E2EKafkaProducer::class.java)
    }
}
