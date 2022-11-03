package no.nav.familie.ks.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.integrasjon.datavarehus.KafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@TaskStepBeskrivelse(
    taskStepType = SendSisteBehandlingstilstandTilDvhTask.TASK_TYPE,
    beskrivelse = "Sending av behandling tilstand til datavarehus"
)
class SendSisteBehandlingstilstandTilDvhTask(private val kafkaProducer: KafkaProducer) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("SendSisteBehandlingstilstandTilDvhTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingStatistikkDto: BehandlingStatistikkDto = objectMapper.readValue(task.payload)
        kafkaProducer.sendSisteBehandlingsTilstand(
            behandlingStatistikkDto.copy(tekniskTidspunkt = OffsetDateTime.now(ZoneOffset.UTC))
        )
    }

    companion object {

        const val TASK_TYPE = "dvh.send.behandlingsatus"
    }
}
