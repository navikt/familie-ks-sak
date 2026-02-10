package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KjøreStatus
import no.nav.familie.ks.sak.kjerne.avstemming.domene.KonsistensavstemmingTaskDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = KonsistensavstemmingTask.TASK_STEP_TYPE,
    beskrivelse = "Start Konsistensavstemming mot oppdrag",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class KonsistensavstemmingTask(
    private val konsistensavstemmingKjøreplanService: KonsistensavstemmingKjøreplanService,
    private val avstemmingService: AvstemmingService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val konsistensavstemmingTaskData = objectMapper.readValue(task.payload, KonsistensavstemmingTaskDto::class.java)
        val avstemmingstidspunkt = LocalDateTime.now()
        val kjøreplanId = konsistensavstemmingTaskData.kjøreplanId
        // Denne må genereres på tasken slik at tasken kan kjøres på nytt som sender ny transasksjonId til økonomi.
        // Eller feiler økonomi siden transkasjonId allerede er brukt opp
        val transaksjonId = UUID.randomUUID()

        // oppdaterer task med transaksjonId som metadata
        task.metadataWrapper.properties["transaksjonId"] = transaksjonId.toString()

        logger.info(
            "Konsistensavstemming med transaksjonId $transaksjonId ble initielt trigget " +
                "${konsistensavstemmingTaskData.initieltKjøreTidspunkt}, men bruker $avstemmingstidspunkt som avstemmingsdato",
        )
        if (konsistensavstemmingKjøreplanService.harKjøreplanStatusFerdig(kjøreplanId)) {
            logger.info(
                "Konsistensavstemmning er allerede kjørt for transaksjonsId=$transaksjonId og " +
                    "kjøreplanId=$kjøreplanId",
            )
            return
        }

        // Start konsistensavstemming start melding til oppdrag
        avstemmingService.sendKonsistensavstemmingStartMelding(avstemmingstidspunkt, transaksjonId)

        // henter relevante behandlinger med 1000 behandlingId per side
        var relevanteBehandlingSider =
            behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
                Pageable.ofSize(ANTALL_BEHANDLINGER),
            )
        for (sideNummer in 1..relevanteBehandlingSider.totalPages) {
            relevanteBehandlingSider.content.chunked(500).forEach { behandlingChunk ->
                val behandlingIder = behandlingChunk.map { it.toLong() }
                val perioderForBehandling =
                    avstemmingService.hentDataForKonsistensavstemming(avstemmingstidspunkt, behandlingIder)
                // Send konsistensavstemming data til oppdrag
                avstemmingService.sendKonsistensavstemmingData(avstemmingstidspunkt, perioderForBehandling, transaksjonId)
            }

            // hent behandlinger fra neste side
            relevanteBehandlingSider =
                behandlingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker(
                    relevanteBehandlingSider.nextPageable(),
                )
        }

        // Send konsistensavstemming avslutt melding til oppdrag
        avstemmingService.sendKonsistensavstemmingAvsluttMelding(avstemmingstidspunkt, transaksjonId)

        // oppdaterer kjøreplan status til FERDIG
        konsistensavstemmingKjøreplanService.lagreNyStatus(kjøreplanId, KjøreStatus.FERDIG)
    }

    companion object {
        const val TASK_STEP_TYPE = "konsistensavstemming"
        const val ANTALL_BEHANDLINGER = 5000
        private val logger = LoggerFactory.getLogger(KonsistensavstemmingTask::class.java)

        fun opprettTask(konsistensavstemmingTaskDto: KonsistensavstemmingTaskDto) =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(konsistensavstemmingTaskDto),
            )
    }
}
