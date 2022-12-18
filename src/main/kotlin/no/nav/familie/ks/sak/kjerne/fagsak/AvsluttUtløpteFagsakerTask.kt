package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = AvsluttUtløpteFagsakerTask.TASK_STEP_TYPE,
    beskrivelse = "Oppdater fagsakstatus fra LØPENDE til AVSLUTTET på avsluttede fagsaker",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60
)
class AvsluttUtløpteFagsakerTask(val fagsakService: FagsakService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val antallOppdaterte = fagsakService.finnOgAvsluttFagsakerSomSkalAvsluttes()
        logger.info("Oppdatert status på $antallOppdaterte fagsaker til ${FagsakStatus.AVSLUTTET.name}")
    }

    companion object {

        const val TASK_STEP_TYPE = "avsluttUtløpteFagsaker"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
        fun lagTask() = Task(type = this.TASK_STEP_TYPE, payload = "")
    }
}
