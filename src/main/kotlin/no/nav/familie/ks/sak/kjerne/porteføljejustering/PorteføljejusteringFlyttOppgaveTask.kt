package no.nav.familie.ks.sak.kjerne.porteføljejustering

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype.NASJONAL
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleUnderkjentVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.GodkjenneVedtak
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.BERGEN
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.KontantstøtteEnhet.VADSØ
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = PorteføljejusteringFlyttOppgaveTask.TASK_STEP_TYPE,
    beskrivelse = "Flytt oppgave til riktig enhet",
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
)
class PorteføljejusteringFlyttOppgaveTask(
    private val integrasjonKlient: IntegrasjonKlient,
    private val behandlingRepository: BehandlingRepository,
    private val personidentService: PersonidentService,
    private val fagsakService: FagsakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) : AsyncTaskStep {
    @WithSpan
    override fun doTask(task: Task) {
        val porteføljejusteringFlyttOppgaveDto = objectMapper.readValue<PorteføljejusteringFlyttOppgaveDto>(task.payload)
        val oppgaveId = porteføljejusteringFlyttOppgaveDto.oppgaveId

        val oppgave = integrasjonKlient.finnOppgaveMedId(oppgaveId)
        if (oppgave.tildeltEnhetsnr !in setOf(VADSØ.enhetsnummer, BERGEN.enhetsnummer)) {
            logger.info("Oppgave med id $oppgaveId er ikke tildelt Vadsø. Avbryter flytting av oppgave.")
            task.metadata["status"] = "Ny enhet for oppgave er ikke Vadsø eller Bergen"
            return
        }

        if (oppgave.behandlingstype != NASJONAL.value) {
            logger.info("Oppgave med id $oppgaveId har ikke behandlingstype NASJONAL. Avbryter flytting av oppgave.")
            task.metadata["status"] = "Oppgave har ikke behandlingstype ${oppgave.behandlingstype}"
            return
        }

        val nyEnhetId = validerOgHentNyEnhetForOppgave(oppgave)
        if (nyEnhetId != BERGEN.enhetsnummer) {
            logger.info("Oppgave med id $oppgaveId skal flyttes til enhet $nyEnhetId. Avbryter flytting av oppgave.")
            task.metadata["status"] = "Ny enhet for oppgave er ikke Bergen"
            return
        }

        oppgave.saksreferanse?.let { task.metadata["saksreferanse"] = it }

        val nyMappeId = hentMappeIdHosBergenSomTilsvarerMappeIVadsø(oppgave.mappeId)

        // Vi oppdaterer bare hvis det er forskjell på enhet eller mappe. Kaster ikke feil grunnet ønske om idempotens.
        val skalOppdatereEnhetEllerMappe = nyMappeId != oppgave.mappeId || nyEnhetId != oppgave.tildeltEnhetsnr
        if (skalOppdatereEnhetEllerMappe) {
            integrasjonKlient.tilordneEnhetOgMappeForOppgave(
                oppgaveId = oppgaveId,
                nyEnhet = nyEnhetId,
                nyMappe = nyMappeId,
            )
            logger.info(
                "Oppdatert oppgave med id $oppgaveId.\n" +
                    "Fra enhet ${oppgave.tildeltEnhetsnr} til ny enhet $nyEnhetId.\n" +
                    "Fra mappe ${oppgave.mappeId} til ny mappe $nyMappeId.",
            )
        }

        // Vi oppdaterer bare behandlingen i ks-sak hvis typen er
        // av BehandleSak, GodkjenndeVedtak eller BehandleUnderkjentVedtak
        if (
            oppgave.saksreferanse != null &&
            oppgave.oppgavetype in setOf(BehandleSak.value, GodkjenneVedtak.value, BehandleUnderkjentVedtak.value)
        ) {
            try {
                oppdaterÅpenBehandlingIKsSak(oppgave, nyEnhetId)
            } catch (e: Exception) {
                integrasjonKlient.tilordneEnhetOgMappeForOppgave(
                    oppgaveId = oppgaveId,
                    nyEnhet = porteføljejusteringFlyttOppgaveDto.originalEnhet,
                    nyMappe = porteføljejusteringFlyttOppgaveDto.originalMappeId,
                )
                logger.info(
                    "Ruller oppdatert oppgave tilbake med id ${porteføljejusteringFlyttOppgaveDto.oppgaveId}.\n" +
                        "Fra enhet $nyEnhetId til original enhet ${porteføljejusteringFlyttOppgaveDto.originalEnhet}.\n" +
                        "Fra mappe $nyMappeId til original mappe ${porteføljejusteringFlyttOppgaveDto.originalMappeId}.",
                )
                throw e
            }
        }
        task.metadata["status"] = "Flytting av oppgave fullført"
        task.metadata["nyEnhetId"] = nyEnhetId
    }

    private fun validerOgHentNyEnhetForOppgave(
        oppgave: Oppgave,
    ): String {
        val ident =
            oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                ?: throw Feil("Oppgave med id ${oppgave.id} er ikke tilknyttet en ident.")

        val arbeidsfordelingsenheter = integrasjonKlient.hentBehandlendeEnheter(ident)
        if (arbeidsfordelingsenheter.isEmpty()) {
            logger.error("Fant ingen arbeidsfordelingsenheter for ident. Se SecureLogs for detaljer.")
            secureLogger.error("Fant ingen arbeidsfordelingsenheter for ident $ident.")
            throw Feil("Fant ingen arbeidsfordelingsenhet for ident.")
        }

        if (arbeidsfordelingsenheter.size > 1) {
            logger.error("Fant flere arbeidsfordelingsenheter for ident. Se SecureLogs for detaljer.")
            secureLogger.error("Fant flere arbeidsfordelingsenheter for ident $ident.")
            throw Feil("Fant flere arbeidsfordelingsenheter for ident.")
        }

        val nyEnhetId = arbeidsfordelingsenheter.single().enhetId
        if (nyEnhetId == VADSØ.enhetsnummer) {
            throw Feil("Oppgave med id ${oppgave.id} tildeles fortsatt Vadsø som enhet")
        }

        return nyEnhetId
    }

    private fun oppdaterÅpenBehandlingIKsSak(
        oppgave: Oppgave,
        nyEnhet: String,
    ) {
        val aktørIdPåOppgave = oppgave.aktoerId ?: throw Feil("Fant ikke aktørId på oppgave for å oppdatere åpen behandling i ks-sak")
        val aktørPåOppgave = personidentService.hentAktør(aktørIdPåOppgave)

        val åpenBehandlingPåAktør =
            fagsakService.hentFagsakForPerson(aktørPåOppgave).let {
                behandlingRepository.findByFagsakAndAktivAndOpen(it.id)
            } ?: throw Feil("Fant ikke åpen behandling på aktør til oppgaveId ${oppgave.id}")

        arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(åpenBehandlingPåAktør, nyEnhet)
    }

    companion object {
        const val TASK_STEP_TYPE = "porteføljejusteringFlyttOppgaveTask"
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(
            oppgaveId: Long,
            enhetId: String,
            mappeId: Long?,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        PorteføljejusteringFlyttOppgaveDto(
                            oppgaveId = oppgaveId,
                            originalEnhet = enhetId,
                            originalMappeId = mappeId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["oppgaveId"] = oppgaveId.toString()
                        this["enhetId"] = enhetId
                        mappeId?.let { this["mappeId"] = it.toString() }
                    },
            )
    }
}
