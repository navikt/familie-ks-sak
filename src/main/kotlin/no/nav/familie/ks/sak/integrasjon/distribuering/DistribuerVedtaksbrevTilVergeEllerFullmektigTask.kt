package no.nav.familie.ks.sak.integrasjon.distribuering

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.task.utledNesteTriggerTidIHverdagerForTask
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTilVergeEllerFullmektigTask.TASK_STEP_TYPE,
    beskrivelse = "Send vedtaksbrev til manuelt registrert verge/fullmektig til Dokdist",
    maxAntallFeil = 3,
)
class DistribuerVedtaksbrevTilVergeEllerFullmektigTask(
    private val brevService: BrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val distribuerBrevDto = objectMapper.readValue(task.payload, DistribuerBrevDto::class.java)
        brevService.prøvDistribuerBrevOgLoggHendelse(
            journalpostId = distribuerBrevDto.journalpostId,
            behandlingId = distribuerBrevDto.behandlingId,
            loggBehandlerRolle = BehandlerRolle.SYSTEM,
            brevmal = distribuerBrevDto.brevmal,
            manuellAdresseInfo = distribuerBrevDto.manuellAdresseInfo,
        )
    }

    companion object {
        fun opprettDistribuerVedtaksbrevTilVergeEllerFullmektigTask(
            distribuerBrevDTO: DistribuerBrevDto,
            properties: Properties,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerBrevDTO),
                properties = properties,
            ).copy(
                triggerTid = utledNesteTriggerTidIHverdagerForTask(),
            )

        const val TASK_STEP_TYPE = "distribuerVedtaksbrevTilVergeEllerFullmektig"
    }
}
