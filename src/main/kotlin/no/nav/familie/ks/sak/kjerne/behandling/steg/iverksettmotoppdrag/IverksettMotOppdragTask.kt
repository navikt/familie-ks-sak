package no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.IverksettMotOppdragDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Iverksett vedtak mot oppdrag", maxAntallFeil = 3)
class IverksettMotOppdragTask(private val stegService: StegService) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val iverksettingData = objectMapper.readValue(task.payload, IverksettMotOppdragDto::class.java)
        stegService.utførSteg(
            behandlingId = iverksettingData.behandlingId,
            behandlingSteg = BehandlingSteg.IVERKSETT_MOT_OPPDRAG,
            behandlingStegDto = iverksettingData
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "iverksettMotOppdrag"

        fun opprettTask(behandling: Behandling, vedtakId: Long, saksbehandlerId: String) = Task(
            type = TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(IverksettMotOppdragDto(behandling.id, saksbehandlerId)),
            properties = Properties().apply {
                this["personIdent"] = behandling.fagsak.aktør.aktivFødselsnummer()
                this["behandlingsId"] = behandling.id.toString()
                this["vedtakId"] = vedtakId.toString()
            }
        )
    }
}
