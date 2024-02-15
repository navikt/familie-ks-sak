package no.nav.familie.ks.sak.internal.endringKontantstøtteInfobrev2024

import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask.TASK_STEP_TYPE,
    beskrivelse = "Opprett fagsak og send informasjonsbrev om kontantstøtteendring",
    maxAntallFeil = 3,
)
class OpprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask(
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val fagsakRepository: FagsakRepository,
    private val personidentRepository: PersonidentRepository,
) : AsyncTaskStep {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun doTask(task: Task) {
        val søkerIdent = task.payload

        val aktørIKs = personidentRepository.findByFødselsnummerOrNull(fødselsnummer = søkerIdent)?.aktør
        if (aktørIKs != null) {
            val fagsakPåSøkerIKs = fagsakRepository.finnFagsakForAktør(aktørIKs)
            if (fagsakPåSøkerIKs != null) {
                logger.info("Søker fra infotrygd finnes allerede i KS på fagsak=$fagsakPåSøkerIKs og skal allerede ha fått brev.")
                return
            }
        }

        secureLogger.info("Oppretter fagsak på person $søkerIdent")
        val minimalFagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = søkerIdent))
        logger.info("Oppretter task for å journalføre og distribuere informasjonsbrev om kontantstøtteendring på fagsak ${minimalFagsak.id}. Saken er originalt fra infotrygd.")
        val sendBrevTask = SendInformasjonsbrevKontantstøtteendringTask.lagTask(fagsakId = minimalFagsak.id)
        taskService.save(sendBrevTask)
    }

    companion object {
        fun lagTask(søkerIdent: String): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = søkerIdent,
                properties = mapOf("fødselsnummerSøker" to søkerIdent).toProperties(),
            )
        }

        const val TASK_STEP_TYPE = "opprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask"
    }
}
