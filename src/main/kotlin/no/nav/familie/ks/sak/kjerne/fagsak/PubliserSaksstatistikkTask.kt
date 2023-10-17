package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.integrasjon.datavarehus.KafkaProducer
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = PubliserSaksstatistikkTask.TASK_STEP_TYPE,
    beskrivelse = "Send fagsakdata til datavarehus",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60,
)
class PubliserSaksstatistikkTask(
    val sakStatistikkService: SakStatistikkService,
    val kafkaProducer: KafkaProducer,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()

        val sakStatistikkDto = sakStatistikkService.mapTilSakDvh(fagsakId)
        kafkaProducer.sendMessageForTopicSak(sakStatistikkDto)

        logger.info("Sender info om fagsak $fagsakId til datavarehus")
    }

    companion object {
        fun lagTask(fagsakId: Long) = Task(type = TASK_STEP_TYPE, payload = objectMapper.writeValueAsString(fagsakId))

        const val TASK_STEP_TYPE = "publiserSaksstatistikk"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
