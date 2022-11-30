package no.nav.familie.ks.sak.statistikk.stønadsstatistikk

import no.nav.familie.ks.sak.integrasjon.datavarehus.KafkaProducer
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Publiser vedtakDVH til kafka Aiven",
    maxAntallFeil = 1
)
class PubliserVedtakTask(
    val kafkaProducer: KafkaProducer,
    val stønadsstatistikkService: StønadsstatistikkService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val vedtakDVH = stønadsstatistikkService.hentVedtakDVH(task.payload.toLong())
        logger.info("Send Vedtak til DVH, behandling id ${vedtakDVH.behandlingsId}")
        task.metadata["offset"] = kafkaProducer.sendMessageForTopicVedtak(vedtakDVH).toString()
    }

    companion object {

        val logger = LoggerFactory.getLogger(PubliserVedtakTask::class.java)
        const val TASK_STEP_TYPE = "publiserVedtakTask"

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
