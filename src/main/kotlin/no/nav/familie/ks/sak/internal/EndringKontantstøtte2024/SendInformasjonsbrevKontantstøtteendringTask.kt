package no.nav.familie.ks.sak.internal.EndringKontantstøtte2024

import no.nav.familie.ks.sak.api.dto.ManueltBrevDto
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendInformasjonsbrevKontantstøtteendringTask.TASK_STEP_TYPE,
    beskrivelse = "Send informasjonsbrev om kontantstøtteendring",
    maxAntallFeil = 3,
)
class SendInformasjonsbrevKontantstøtteendringTask(
    private val brevService: BrevService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsak = fagsakService.hentFagsak(fagsakId = task.payload.toLong())

        val manueltBrevDto =
            ManueltBrevDto(
                brevmal = Brevmal.INFORMASJONSBREV_MULIG_LOVENDRING,
                mottakerIdent = fagsak.aktør.aktivFødselsnummer(),
                // Dette brevet skal kun sendes ut på bokmål
                mottakerMålform = Målform.NB,
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
                properties =
                    Properties().apply {
                        this["fagsakId"] = fagsakId
                    },
            )
        }

        const val TASK_STEP_TYPE = "sendInformasjonsbrevKontantstøtteendring"
    }
}
