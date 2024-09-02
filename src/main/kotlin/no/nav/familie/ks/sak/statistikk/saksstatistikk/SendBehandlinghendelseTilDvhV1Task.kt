package no.nav.familie.ks.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.integrasjon.datavarehus.KafkaProducer
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Deprecated("Kan slettes når siste task for å sende er kjørt")
@Service
@TaskStepBeskrivelse(
    taskStepType = SendBehandlinghendelseTilDvhV1Task.TASK_TYPE,
    beskrivelse = "Sending av behandlinghendelse til datavarehus",
)
class SendBehandlinghendelseTilDvhV1Task(
    private val kafkaProducer: KafkaProducer,
) : AsyncTaskStep {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("SendBehandlinghendelseTilDvhTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingStatistikkV1Dto: BehandlingStatistikkV1Dto = objectMapper.readValue(task.payload)
        val tekniskTidspunkt =
            OffsetDateTime.of(
                LocalDateTime.now(),
                ZoneOffset.UTC,
            )
        // Logger om teknisk tidspunkt er tidligere enn funksjonell tidspunkt, da dette ikke skal forekomme. Men datavarehus har rapportert om det
        if (tekniskTidspunkt.isBefore(behandlingStatistikkV1Dto.funksjoneltTidspunkt)) {
            log.warn("Teknisk tidspunkt er tidligere enn funksjonell tidspunkt. Teknisk tidspunkt: ${behandlingStatistikkV1Dto.tekniskTidspunkt}, funksjonell tidspunkt: $tekniskTidspunkt Tidssone ${LocaleContextHolder.getTimeZone()}")
        }
        kafkaProducer.sendBehandlingsTilstand(
            behandlingStatistikkV1Dto.behandlingID.toString(),
            behandlingStatistikkV1Dto.copy(tekniskTidspunkt = tekniskTidspunkt),
        )
    }

    companion object {
        const val TASK_TYPE = "dvh.send.behandlinghendelse"
    }
}
