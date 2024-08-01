package no.nav.familie.ks.sak.internal.kontantstøtteInfobrevJuli2024

import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SendInformasjonsbrevKontantstøtteendringTask.TASK_STEP_TYPE,
    beskrivelse = "Send informasjonsbrev om kontantstøtteendring",
    maxAntallFeil = 1,
)
class SendInformasjonsbrevKontantstøtteendringTask(
    private val brevService: BrevService,
    private val fagsakService: FagsakService,
    private val personOpplysningerService: PersonOpplysningerService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val alleTaskerMedSammePayloadOgType =
            taskService.finnAlleTaskerMedPayloadOgType(payload = task.payload, type = task.type)
        if (alleTaskerMedSammePayloadOgType.size >= 2) {
            throw Feil("Det finnes flere tasker med samme payload og type på fagsak ${task.payload}")
        }

        val fagsak = fagsakService.hentFagsak(fagsakId = task.payload.toLong())
        val person = personOpplysningerService.hentPersoninfoEnkel(fagsak.aktør)

        val manueltBrevDto =
            ManueltBrevDto(
                brevmal = Brevmal.INFORMASJONSBREV_LOVENDRING_JULI_2024,
                mottakerIdent = fagsak.aktør.aktivFødselsnummer(),
                // Dette brevet skal kun sendes ut på bokmål
                mottakerMålform = Målform.NB,
                mottakerNavn = person.navn ?: throw Feil("Fant ikke navn på person"),
            )

        brevService.sendBrev(
            manueltBrevDto = manueltBrevDto,
            fagsak = fagsak,
        )
    }

    companion object {
        fun lagTask(fagsakId: Long): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = fagsakId.toString(),
                properties = mapOf("fagsakId" to fagsakId.toString()).toProperties(),
            )
        }

        const val TASK_STEP_TYPE = "sendInformasjonsbrevKontantstøtteJuli2024"
    }
}
