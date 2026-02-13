package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.ks.sak.common.exception.Feil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

// Sender ks-sak behandling data til familie-tilbake via kafka
interface KafkaProducer {
    fun sendFagsystemsbehandlingRespons(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    )
}

@Service
@Profile("!integrasjonstest & !dev-postgres-preprod & !postgres")
class HentFagsystemsbehandlingResponsKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${TILBAKEKREVING_RESPONSE_TOPIC}")
    val tilbakekrevingResponseTopic: String,
) : KafkaProducer {
    override fun sendFagsystemsbehandlingRespons(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        val meldingIString: String = jsonMapper.writeValueAsString(melding)
        kafkaTemplate
            .send(tilbakekrevingResponseTopic, key, meldingIString)
            .thenAccept {
                logger.info(
                    """Melding på topic $tilbakekrevingResponseTopic for $behandlingId med $key er sendt. 
                            Fikk offset ${it?.recordMetadata?.offset()}
                    """.trimMargin(),
                )
            }.exceptionally {
                val feilmelding =
                    """Melding på topic $tilbakekrevingResponseTopic kan ikke sendes for $behandlingId 
                            med $key. Feiler med ${it.message}
                    """.trimMargin()
                logger.warn(feilmelding)
                throw Feil(message = feilmelding)
            }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HentFagsystemsbehandlingResponsKafkaProducer::class.java)
    }
}

@Service
@Profile("postgres", "integrasjonstest", "dev-postgres-preprod", "postgres")
class DummyHentFagsystemsbehandlingResponsKafkaProducer : KafkaProducer {
    override fun sendFagsystemsbehandlingRespons(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        logger.info("Skipper sending av fagsystemsbehandling respons for $behandlingId fordi kafka ikke er enablet")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DummyHentFagsystemsbehandlingResponsKafkaProducer::class.java)
    }
}
