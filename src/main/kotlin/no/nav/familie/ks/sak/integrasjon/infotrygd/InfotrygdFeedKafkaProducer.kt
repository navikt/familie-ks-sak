package no.nav.familie.ks.sak.integrasjon.infotrygd

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.ks.infotrygd.feed.StartBehandlingDto
import no.nav.familie.kontrakter.ks.infotrygd.feed.VedtakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.KafkaConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

interface KafkaProducer {
    fun sendStartBehandlingHendelseTilInfotrygd(startBehandlingDto: StartBehandlingDto)

    fun sendVedtakHendelseTilInfotrygd(vedtakDto: VedtakDto)
}

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod & !postgres")
class InfotrygdFeedKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : KafkaProducer {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    override fun sendStartBehandlingHendelseTilInfotrygd(startBehandlingDto: StartBehandlingDto) {
        sendKafkamelding(
            personIdent = startBehandlingDto.fnrStoenadsmottaker,
            key = UUID.randomUUID().toString(),
            request = startBehandlingDto,
        )
    }

    override fun sendVedtakHendelseTilInfotrygd(vedtakDto: VedtakDto) {
        sendKafkamelding(
            personIdent = vedtakDto.fnrStoenadsmottaker,
            key = UUID.randomUUID().toString(),
            request = vedtakDto,
        )
    }

    private fun sendKafkamelding(
        personIdent: String,
        key: String,
        request: Any,
    ) {
        val topic = KafkaConfig.KONTANTSTØTTE_FEED_TOPIC
        val melding = objectMapper.writeValueAsString(request)
        val producerRecord = ProducerRecord(topic, key, melding)

        kafkaTemplate
            .send(producerRecord)
            .thenAccept {
                secureLogger.info(
                    "Melding på topic $topic for $personIdent med $key er sendt. " +
                        "Fikk offset ${it?.recordMetadata?.offset()}",
                )
            }.exceptionally {
                val feilmelding =
                    "Melding på topic $topic kan ikke sendes for $personIdent med $key. " +
                        "Feiler med ${it.message}"
                secureLogger.warn(feilmelding)
                throw Feil(message = feilmelding)
            }
    }
}

@Service
@Profile("postgres", "integrasjonstest", "dev-postgres-preprod", "postgres")
class DummyInfotrygdFeedKafkaProducer : KafkaProducer {
    override fun sendStartBehandlingHendelseTilInfotrygd(startBehandlingDto: StartBehandlingDto) {
        secureLogger.info(
            "Skipper sending av saksstatistikk for ${startBehandlingDto.fnrStoenadsmottaker} fordi kafka ikke er enablet",
        )
    }

    override fun sendVedtakHendelseTilInfotrygd(vedtakDto: VedtakDto) {
        secureLogger.info(
            "Skipper sending av saksstatistikk for ${vedtakDto.fnrStoenadsmottaker} fordi kafka ikke er enablet",
        )
    }

    companion object {
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
