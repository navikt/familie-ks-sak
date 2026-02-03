package no.nav.familie.ks.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ks.sak.integrasjon.datavarehus.KafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.readValue
import java.time.ZonedDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = SendSisteBehandlingstilstandTilDvhTask.TASK_TYPE,
    beskrivelse = "Sending av behandling tilstand til datavarehus",
)
class SendSisteBehandlingstilstandTilDvhTask(
    private val kafkaProducer: KafkaProducer,
) : AsyncTaskStep {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("SendSisteBehandlingstilstandTilDvhTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingStatistikkV2Dto: BehandlingStatistikkV2Dto =
            sakstatistikkJsonMapper.readValue(task.payload)

        kafkaProducer.sendSisteBehandlingsTilstand(
            behandlingStatistikkV2Dto.copy(tekniskTidspunkt = ZonedDateTime.now()),
        )
    }

    companion object {
        const val TASK_TYPE = "dvh.send.behandlingsatus"
    }
}
