package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KjøreStatus
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingTaskDto
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemmingTask.TASK_STEP_TYPE,
    beskrivelse = "Start Konsistensavstemming mot oppdrag",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true
)
class KonsistensavstemmingTask(
    private val konsistensavstemmingKjøreplanService: KonsistensavstemmingKjøreplanService,
    private val avstemmingService: AvstemmingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val konsistensavstemmingTaskData = objectMapper.readValue(task.payload, KonsistensavstemmingTaskDto::class.java)
        val avstemmingstidspunkt = LocalDateTime.now()
        val kjøreplanId = konsistensavstemmingTaskData.kjøreplanId
        val transaksjonId = konsistensavstemmingTaskData.transaksjonId

        logger.info(
            "Konsistensavstemming ble initielt trigget ${konsistensavstemmingTaskData.initieltKjøreTidspunkt}, " +
                "men bruker $avstemmingstidspunkt som avstemmingsdato"
        )
        if (konsistensavstemmingKjøreplanService.harKjøreplanStatusFerdig(kjøreplanId)) {
            logger.info(
                "Konsistensavstemmning er allerede kjørt for transaksjonsId=$transaksjonId og " +
                    "kjøreplanId=$kjøreplanId"
            )
            return
        }

        // Start konsistensavstemming start melding til oppdrag
        avstemmingService.sendKonsistensavstemmingStartMelding(avstemmingstidspunkt, transaksjonId)

        // henter relevante behandlinger med 1000 behandlingId per side
        var relevanteBehandlingSider = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
            Pageable.ofSize(ANTALL_BEHANDLINGER)
        )
        for (sideNummer in 1..relevanteBehandlingSider.totalPages) {
            val behandlingIder = relevanteBehandlingSider.content.map { it.toLong() }
            val perioderForBehandling = avstemmingService.hentDataForKonsistensavstemming(avstemmingstidspunkt, behandlingIder)
            // Send konsistensavstemming data til oppdrag
            avstemmingService.sendKonsistensavstemmingData(avstemmingstidspunkt, perioderForBehandling, transaksjonId)
            // hent behandlinger fra neste side
            relevanteBehandlingSider = avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
                relevanteBehandlingSider.nextPageable()
            )
        }

        // Send konsistensavstemming avslutt melding til oppdrag
        avstemmingService.sendKonsistensavstemmingAvsluttMelding(avstemmingstidspunkt, transaksjonId)

        // oppdaterer kjøreplan status til FERDIG
        konsistensavstemmingKjøreplanService.lagreNyStatus(kjøreplanId, KjøreStatus.FERDIG)
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemming"
        const val ANTALL_BEHANDLINGER = 1000
        private val logger = LoggerFactory.getLogger(KonsistensavstemmingTask::class.java)

        fun opprettTask(konsistensavstemmingTaskDto: KonsistensavstemmingTaskDto) = Task(
            type = TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(KonsistensavstemmingTaskDto::class.java),
            properties = Properties().apply {
                this["transaksjonsId"] = konsistensavstemmingTaskDto.transaksjonId
            }
        )
    }
}
