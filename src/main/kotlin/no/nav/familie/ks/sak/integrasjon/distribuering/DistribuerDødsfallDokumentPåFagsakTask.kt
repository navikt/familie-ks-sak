package no.nav.familie.ks.sak.integrasjon.distribuering

import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.brev.BrevService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.mottakerErDødUtenDødsboadresse
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.restklient.client.RessursException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

const val ANTALL_SEKUNDER_I_EN_UKE = 604800L

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerDødsfallBrevPåFagsakTask.TASK_STEP_TYPE,
    beskrivelse = "Send dødsfall dokument til Dokdist",
    triggerTidVedFeilISekunder = ANTALL_SEKUNDER_I_EN_UKE,
    // ~8 måneder dersom vi prøver én gang i uka.
    // Tasken skal stoppe etter 6 måneder, så om vi kommer hit har det skjedd noe galt.
    maxAntallFeil = 4 * 8,
    settTilManuellOppfølgning = true,
)
class DistribuerDødsfallBrevPåFagsakTask(
    private val brevService: BrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val distribuerDødsfallBrevPåFagsakTask =
            jsonMapper.readValue(task.payload, DistribuerDødsfallBrevPåFagsakDTO::class.java)

        val journalpostId = distribuerDødsfallBrevPåFagsakTask.journalpostId
        val brevmal = distribuerDødsfallBrevPåFagsakTask.brevmal

        val erTaskEldreEnn6Mnd = task.opprettetTid.isBefore(LocalDateTime.now().minusMonths(6))

        if (erTaskEldreEnn6Mnd) {
            logger.info("Stopper \"DistribuerDødsfallBrevPåFagsakTask\" fordi den er eldre enn 6 måneder.")
        } else {
            try {
                brevService.prøvDistribuerBrevOgLoggHendelse(
                    journalpostId = journalpostId,
                    behandlingId = null,
                    loggBehandlerRolle = BehandlerRolle.SYSTEM,
                    brevmal = brevmal,
                )
            } catch (e: Exception) {
                if (e is RessursException && mottakerErDødUtenDødsboadresse(e)) {
                    logger.info(
                        "Klarte ikke å distribuere \"${brevmal.visningsTekst}\" på journalpost $journalpostId. Prøver igjen om 7 dager.",
                    )
                } else {
                    logger.warn("Feilet med å distribuere brev. Mottaker er ikke død uten dødsboadresse. task=${task.id} journalpostId=$journalpostId")
                    secureLogger.warn("Feilet med å distribuere brev. Mottaker er ikke død uten dødsboadresse. task=${task.id} journalpostId=$journalpostId", e)
                }
                throw e
            }
        }
    }

    companion object {
        fun opprettTask(
            journalpostId: String,
            brevmal: Brevmal,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        DistribuerDødsfallBrevPåFagsakDTO(
                            journalpostId,
                            brevmal,
                        ),
                    ),
            )

        const val TASK_STEP_TYPE = "distribuerBrevPåFagsak"
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class DistribuerDødsfallBrevPåFagsakDTO(
    val journalpostId: String,
    val brevmal: Brevmal,
)
