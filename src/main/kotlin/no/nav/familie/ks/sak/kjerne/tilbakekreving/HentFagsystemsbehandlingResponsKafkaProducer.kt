package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.KafkaConfig.Companion.FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC
import org.slf4j.LoggerFactory
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
@Profile("!integrasjonstest & !dev-postgres-preprod")
class HentFagsystemsbehandlingResponsKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) : KafkaProducer {
    override fun sendFagsystemsbehandlingRespons(
        melding: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        val meldingIString: String = objectMapper.writeValueAsString(melding)
        kafkaTemplate
            .send(FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC, key, meldingIString)
            .thenAccept {
                logger.info(
                    """Melding på topic $FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC for $behandlingId med $key er sendt. 
                            Fikk offset ${it?.recordMetadata?.offset()}
                    """.trimMargin(),
                )
            }.exceptionally {
                val feilmelding =
                    """Melding på topic $FAGSYSTEMSBEHANDLING_RESPONS_TBK_TOPIC kan ikke sendes for $behandlingId 
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
@Profile("postgres", "integrasjonstest", "dev-postgres-preprod")
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
