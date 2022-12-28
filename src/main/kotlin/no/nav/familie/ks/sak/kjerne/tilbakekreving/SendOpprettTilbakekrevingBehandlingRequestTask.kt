package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendOpprettTilbakekrevingBehandlingRequestTask.TASK_STEP_TYPE,
    beskrivelse = "Kaller familie-tilbake for å sende OpprettTilbakekreving request",
    maxAntallFeil = 3
)
class SendOpprettTilbakekrevingBehandlingRequestTask(
    private val behandlingRepository: BehandlingRepository,
    private val tilbakekrevingService: TilbakekrevingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()
        val behandling = behandlingRepository.hentBehandling(behandlingId)

        logger.info("Oppretter tilbakekrevingsbehandling for behandling $behandlingId")
        val tilbakekrevingsbehandlingId = tilbakekrevingService.sendOpprettTilbakekrevingRequest(behandling)

        // Oppdater tilbakekreving tabell med tilbakekrevingsbehandlingId
        tilbakekrevingService.oppdaterTilbakekreving(tilbakekrevingsbehandlingId, behandlingId)
    }

    companion object {
        const val TASK_STEP_TYPE = "send.opprett.tilbakekrevingsbehandling.request"
        private val logger = LoggerFactory.getLogger(SendOpprettTilbakekrevingBehandlingRequestTask::class.java)

        fun opprettTask(behandlingId: Long) = Task(
            type = TASK_STEP_TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply { this["behandlingsId"] = behandlingId.toString() }
        )
    }
}
