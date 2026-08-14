package no.nav.familie.ks.sak.task

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.log.IdUtils
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = GjenåpneFagsakIDokarkivTask.TASK_STEP_TYPE,
    beskrivelse = "Gjenåpne sak i dokarkiv etter opplåsing av fagsak",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class GjenåpneFagsakIDokarkivTask(
    private val fagsakRepository: FagsakRepository,
    private val integrasjonKlient: IntegrasjonKlient,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Fant ikke fagsak $fagsakId")

        integrasjonKlient.gjenåpneSakIDokarkiv(
            GjenåpneSakRequest(
                tema = Tema.KON,
                fagsakId = fagsakId.toString(),
                fagsaksystem = Fagsystem.KONT,
                bruker =
                    DokarkivBruker(
                        idType = BrukerIdType.FNR,
                        id = fagsak.aktør.aktivFødselsnummer(),
                    ),
            ),
        )
    }

    companion object {
        const val TASK_STEP_TYPE = "gjenåpneFagsakIDokarkivTask"

        fun opprettTask(fagsakId: Long): Task =
            overstyrTaskMedNyCallId(IdUtils.generateId()) {
                Task(
                    type = TASK_STEP_TYPE,
                    payload = fagsakId.toString(),
                )
            }
    }
}
