package no.nav.familie.ks.sak.kjerne.porteføljejustering

import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.toString

@Service
class PorteføljejusteringService(
    private val integrasjonKlient: IntegrasjonKlient,
    private val taskService: TaskService,
) {
    fun lagTaskForOverføringAvOppgaverFraVadsø(
        antallTasks: Int? = null,
        dryRun: Boolean = true,
    ): Pair<Int, Int> {
        val oppgaverIVadsø =
            integrasjonKlient
                .hentOppgaver(
                    finnOppgaveRequest =
                        FinnOppgaveRequest(
                            tema = Tema.KON,
                            enhet = KontantstøtteEnhet.VADSØ.enhetsnummer,
                        ),
                ).oppgaver

        logger.info("Fant ${oppgaverIVadsø.size} kontantstøtte oppgaver i Vadsø")

        val oppgaverSomSkalFlyttes =
            oppgaverIVadsø
                .filterNot { it.saksreferanse?.matches("\\d+[A-Z]\\d+".toRegex()) == true } // Filtrere bort infotrygd-oppgaver
                .filterNot { it.mappeId == null } // Vi skal ikke flytte oppgaver som ikke har mappe id
                .filter { it.behandlingstype == Behandlingstype.NASJONAL.value }

        logger.info("Fant ${oppgaverSomSkalFlyttes.size} kontantstøtte oppgaver som skal flyttes")

        val totalAntallOppgaverSomSkalFlyttes = oppgaverSomSkalFlyttes.size
        var opprettedeTasks = 0

        if (!dryRun) {
            oppgaverSomSkalFlyttes
                .take(antallTasks ?: oppgaverSomSkalFlyttes.size)
                .forEach { oppgave ->
                    oppgave.id?.let {
                        PorteføljejusteringFlyttOppgaveTask.opprettTask(
                            oppgaveId = it,
                            enhetId = oppgave.tildeltEnhetsnr,
                            mappeId = oppgave.mappeId?.toString(),
                        )
                        opprettedeTasks++
                    }
                }
        }

        return totalAntallOppgaverSomSkalFlyttes to opprettedeTasks
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PorteføljejusteringService::class.java)
    }
}
