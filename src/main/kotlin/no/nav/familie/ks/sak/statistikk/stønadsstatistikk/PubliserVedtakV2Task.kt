package no.nav.familie.ks.sak.statistikk.stønadsstatistikk

import no.nav.familie.ks.sak.integrasjon.datavarehus.KafkaProducer
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakV2Task.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Publiser vedtak V2 til kafka Aiven",
    maxAntallFeil = 1
)
class PubliserVedtakV2Task(
    val kafkaProducer: KafkaProducer,
    val stønadsstatistikkService: StønadsstatistikkService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakV2DVH = stønadsstatistikkService.hentVedtakV2(task.payload.toLong())
        LOG.info("Send VedtakV2 til DVH, behandling id ${vedtakV2DVH.behandlingsId}")
        task.metadata["offset"] = kafkaProducer.sendMessageForTopicVedtakV2(vedtakV2DVH).toString()
    }

    companion object {

        val LOG = LoggerFactory.getLogger(PubliserVedtakV2Task::class.java)
        const val TASK_STEP_TYPE = "publiserVedtakV2Task"

        fun opprettTask(personIdent: String, behandlingsId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = behandlingsId.toString(),
                properties = Properties().apply {
                    this["personIdent"] = personIdent
                    this["behandlingsId"] = behandlingsId.toString()
                }
            )
        }
    }
}
