package no.nav.familie.ks.sak.integrasjon.distributering

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.DistribuerBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.task.nesteGyldigeTriggertidForBehandlingIHverdager
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send dokument til Dokdist", maxAntallFeil = 3)
class DistribuerBrevTask(
    private val behandlingService: BehandlingService,
    private val brevService: BrevService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerBrevDto = objectMapper.readValue(task.payload, DistribuerBrevDto::class.java)

        if (distribuerBrevDto.erManueltSendt && !distribuerBrevDto.brevmal.erVedtaksbrev) {
            brevService.prøvDistribuerBrevOgLoggHendelse(
                journalpostId = distribuerBrevDto.journalpostId,
                behandlingId = distribuerBrevDto.behandlingId,
                loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                brevmal = distribuerBrevDto.brevmal
            )
        } else if (!distribuerBrevDto.erManueltSendt && distribuerBrevDto.brevmal.erVedtaksbrev && distribuerBrevDto.behandlingId != null) {
            brevService.prøvDistribuerBrevOgLoggHendelse(
                journalpostId = distribuerBrevDto.journalpostId,
                behandlingId = distribuerBrevDto.behandlingId,
                loggBehandlerRolle = BehandlerRolle.SYSTEM,
                brevmal = distribuerBrevDto.brevmal
            )

            val behandling = behandlingService.hentBehandling(distribuerBrevDto.behandlingId)
            val søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer()

            // TODO: Legg til ferdigstilling av behandling
            // val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
            //     søkerIdent = søkerIdent,
            //     behandlingsId = behandling.id
            // )
            //
            // taskRepository.save(ferdigstillBehandlingTask)
        } else {
            throw Feil("erManueltSendt=${distribuerBrevDto.erManueltSendt} ikke støttet for brev=${distribuerBrevDto.brevmal.visningsTekst}")
        }
    }

    companion object {

        fun opprettDistribuerBrevTask(
            distribuerBrevDTO: DistribuerBrevDto,
            properties: Properties
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerBrevDTO),
                properties = properties
            ).copy(
                triggerTid = nesteGyldigeTriggertidForBehandlingIHverdager()
            )
        }

        private const val TASK_STEP_TYPE = "distribuerBrev"
    }
}
