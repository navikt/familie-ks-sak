package no.nav.familie.ks.sak.integrasjon.datavarehus

import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.KafkaConfig
import no.nav.familie.ks.sak.statistikk.saksstatistikk.BehandlingStatistikkV2Dto
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkDto
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

interface KafkaProducer {
    fun sendBehandlingsTilstand(
        behandlingId: String,
        request: BehandlingStatistikkV2Dto,
    )

    fun sendSisteBehandlingsTilstand(request: BehandlingStatistikkV2Dto)

    fun sendMessageForTopicVedtak(vedtak: VedtakDVH)

    fun sendMessageForTopicSak(sakStatistikkDto: SakStatistikkDto)
}

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod & !postgres")
class DatavarehusKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : KafkaProducer {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun sendBehandlingsTilstand(
        behandlingId: String,
        request: BehandlingStatistikkV2Dto,
    ) {
        sendKafkamelding(
            topic = KafkaConfig.BEHANDLING_TOPIC,
            key = request.behandlingID.toString(),
            request = request,
            behandlingId = behandlingId,
            fagsakId = request.saksnummer.toString(),
        )
    }

    override fun sendMessageForTopicVedtak(vedtak: VedtakDVH) {
        sendKafkamelding(
            topic = KafkaConfig.VEDTAK_TOPIC,
            key = vedtak.funksjonellId,
            request = vedtak,
            behandlingId = vedtak.behandlingsId,
            fagsakId = vedtak.fagsakId,
        )
    }

    override fun sendSisteBehandlingsTilstand(request: BehandlingStatistikkV2Dto) {
        sendKafkamelding(
            topic = KafkaConfig.SISTE_TILSTAND_BEHANDLING_TOPIC,
            key = request.behandlingID.toString(),
            request = request,
            behandlingId = request.behandlingID.toString(),
            fagsakId = request.saksnummer.toString(),
        )
    }

    override fun sendMessageForTopicSak(sakStatistikkDto: SakStatistikkDto) {
        sendKafkamelding(
            topic = KafkaConfig.SAK_TOPIC,
            key = sakStatistikkDto.funksjonellId,
            request = sakStatistikkDto,
            fagsakId = sakStatistikkDto.sakId,
        )
    }

    private fun sendKafkamelding(
        topic: String,
        key: String,
        request: Any,
        behandlingId: String? = null,
        fagsakId: String? = null,
    ) {
        val melding = objectMapper.writeValueAsString(request)

        val logMeldingMetadata =
            "Topicnavn: $topic \n" +
                "Nøkkel: $key \n" +
                "BehandlingId: $behandlingId \n" +
                "FagsakId: $fagsakId \n"

        kafkaTemplate
            .send(topic, key, melding)
            .thenAccept { log.info("Melding sendt på kafka. \n" + "Offset: ${it?.recordMetadata?.offset()} \n" + logMeldingMetadata) }
            .exceptionally { throw Feil("Kafkamelding kan ikke sendes. \n" + logMeldingMetadata + "Feilmelding: \"${it.message}\"") }
    }
}

@Service
@Profile("postgres", "integrasjonstest", "dev-postgres-preprod", "postgres")
class DummyDatavarehusKafkaProducer : KafkaProducer {
    override fun sendBehandlingsTilstand(
        behandlingId: String,
        request: BehandlingStatistikkV2Dto,
    ) {
        log.info("Skipper sending av saksstatistikk for behandling ${request.behandlingID} fordi kafka ikke er enablet")
    }

    override fun sendSisteBehandlingsTilstand(request: BehandlingStatistikkV2Dto) {
        log.info("Skipper sending av saksstatistikk for behandling ${request.behandlingID} fordi kafka ikke er enablet")
    }

    override fun sendMessageForTopicVedtak(vedtak: VedtakDVH) {
        log.info("Skipper sending av vedtak for behandling ${vedtak.behandlingsId} fordi kafka ikke er enablet")
    }

    override fun sendMessageForTopicSak(sakStatistikkDto: SakStatistikkDto) {
        log.info("Skipper sending av saksstatistikk for fagsak ${sakStatistikkDto.sakId} fordi kafka ikke er enablet")
    }

    companion object {
        private val log = LoggerFactory.getLogger(DummyDatavarehusKafkaProducer::class.java)
    }
}
