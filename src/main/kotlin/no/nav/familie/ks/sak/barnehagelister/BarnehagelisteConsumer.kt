package no.nav.familie.ks.sak.barnehagelister

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.config.KafkaConfig
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

@Service
@Profile("!dev-postgres-preprod & !postgres")
class BarnehagelisteConsumer(
    val barnehageBarnService: BarnehagebarnService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(
        id = "familie-ks-barnehagelister",
        groupId = "familie-ks-barnehagelister-group",
        topics = [KafkaConfig.BARNEHAGELISTE_TOPIC],
    )
    fun listen(
        message: String,
        ack: Acknowledgment,
    ) {
        val barnehagebarn: Barnehagebarn = objectMapper.readValue(message, Barnehagebarn::class.java)

        logger.info("Barnehagebarn mottatt på kafka med id ${barnehagebarn.id}")

        // Sjekk at vi ikke har mottat meldingen tidligere
        if (barnehageBarnService.erBarnehageBarnMottattTidligere(barnehagebarn)) {
            logger.info("Barnehagebarn med id ${barnehagebarn.id} er mottatt tidligere. Hopper over")
            ack.acknowledge()
            return
        }

        barnehageBarnService.lagreBarnehageBarn(barnehagebarn)
        ack.acknowledge()
    }
}
