package no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev

import no.nav.familie.ks.sak.api.dto.JournalførVedtaksbrevDTO
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.journalførvedtaksbrev.JournalførVedtaksbrevTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Journalfør brev i Joark", maxAntallFeil = 3)
class JournalførVedtaksbrevTask(private val stegService: StegService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = task.payload.toLong()
        val vedtakId = task.metadata.getProperty("vedtakId").toLong()

        stegService.utførSteg(
            behandlingId = behandlingId,
            behandlingSteg = BehandlingSteg.JOURNALFØR_VEDTAKSBREV,
            behandlingStegDto = JournalførVedtaksbrevDTO(vedtakId = vedtakId, task = task)
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "journalførVedtaksbrev"

        fun opprettTask(behandling: Behandling, vedtakId: Long) = Task(
            type = TASK_STEP_TYPE,
            payload = "${behandling.id}",
            properties = Properties().apply {
                this["personIdent"] = behandling.fagsak.aktør.aktivFødselsnummer()
                this["behandlingsId"] = behandling.id.toString()
                this["vedtakId"] = vedtakId.toString()
            }
        )
    }
}
