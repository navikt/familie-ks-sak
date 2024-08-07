package no.nav.familie.ks.sak.kjerne.behandling

import no.nav.familie.kontrakter.ks.infotrygd.feed.StartBehandlingDto
import no.nav.familie.ks.sak.integrasjon.infotrygd.KafkaProducer
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendStartBehandlingHendelseTilInfotrygdTask.TASK_STEP_TYPE,
    beskrivelse = "Send startbehandling hendelse til Infotrygd feed.",
)
class SendStartBehandlingHendelseTilInfotrygdTask(
    private val kafkaProducer: KafkaProducer,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val søkersfnr = task.payload
        secureLogger.info("Sender StartBehandling hendelse for $søkersfnr via Kafka")
        kafkaProducer.sendStartBehandlingHendelseTilInfotrygd(StartBehandlingDto(task.payload))
    }

    companion object {
        const val TASK_STEP_TYPE = "sendStartBehandlingHendelseTilInfotrygd"
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(aktørStønadsmottaker: Aktør): Task {
            secureLogger.info(
                "Oppretter task for å sende StartBehandling for " +
                    "${aktørStønadsmottaker.aktivFødselsnummer()} til Infotrygd.",
            )
            return Task(
                type = TASK_STEP_TYPE,
                payload = aktørStønadsmottaker.aktivFødselsnummer(),
                properties =
                    Properties().apply {
                        this["personIdent"] = aktørStønadsmottaker.aktivFødselsnummer()
                    },
            )
        }
    }
}
