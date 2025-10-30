package no.nav.familie.ks.sak.integrasjon.distribuering

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.avsluttbehandling.AvsluttBehandlingTask
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.task.utledNesteTriggerTidIHverdagerForTask
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerBrevTask.TASK_STEP_TYPE,
    beskrivelse = "Send dokument til Dokdist",
    maxAntallFeil = 3,
)
class DistribuerBrevTask(
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val taskRepositoryWrapper: TaskRepositoryWrapper,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val distribuerBrevDto = objectMapper.readValue(task.payload, DistribuerBrevDto::class.java)

        if (distribuerBrevDto.erManueltSendt && !distribuerBrevDto.brevmal.erVedtaksbrev) {
            brevService.prøvDistribuerBrevOgLoggHendelse(
                journalpostId = distribuerBrevDto.journalpostId,
                behandlingId = distribuerBrevDto.behandlingId,
                loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                brevmal = distribuerBrevDto.brevmal,
                manuellAdresseInfo = distribuerBrevDto.manuellAdresseInfo,
            )
        } else if (!distribuerBrevDto.erManueltSendt &&
            distribuerBrevDto.brevmal.erVedtaksbrev &&
            distribuerBrevDto.behandlingId != null
        ) {
            brevService.prøvDistribuerBrevOgLoggHendelse(
                journalpostId = distribuerBrevDto.journalpostId,
                behandlingId = distribuerBrevDto.behandlingId,
                loggBehandlerRolle = BehandlerRolle.SYSTEM,
                brevmal = distribuerBrevDto.brevmal,
                manuellAdresseInfo = distribuerBrevDto.manuellAdresseInfo,
            )

            val behandling = behandlingService.hentBehandling(distribuerBrevDto.behandlingId)
            val søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer()

            val avsluttBehandlingTask =
                AvsluttBehandlingTask.opprettTask(
                    søkerIdent = søkerIdent,
                    behandlingId = behandling.id,
                )
            taskRepositoryWrapper.save(avsluttBehandlingTask)
        } else {
            throw Feil(
                "erManueltSendt=${distribuerBrevDto.erManueltSendt} " +
                    "ikke støttet for brev=${distribuerBrevDto.brevmal.visningsTekst}",
            )
        }
    }

    companion object {
        fun opprettDistribuerBrevTask(
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

        const val TASK_STEP_TYPE = "distribuerBrev"
    }
}
