package no.nav.familie.ks.sak.internal.EndringKontantstøtte2024

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.Fødselsnummer
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.api.dto.FagsakRequestDto
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.infotrygd.SøkerOgBarn
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
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask.TASK_STEP_TYPE,
    beskrivelse = "Opprett fagsagk og send informasjonsbrev om kontantstøtteendring",
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
        val søkerOgBarn = objectMapper.readValue<SøkerOgBarn>(task.payload)

        val barnasFødselsdatoer = søkerOgBarn.barnIdenter.map { Fødselsnummer(it).fødselsdato }
        val erBarnFødtEtterSeptember22 = barnasFødselsdatoer.any { it.toYearMonth() >= YearMonth.of(2022, 9) }

        if (!erBarnFødtEtterSeptember22) {
            secureLogger.info("Ingen barn født etter september 2022 for $søkerOgBarn. Sender ikke infobrev.")
            return
        }

        val aktørIKS = personidentRepository.findByFødselsnummerOrNull(fødselsnummer = søkerOgBarn.søkerIdent)?.aktør
        if (aktørIKS != null) {
            val fagsakPåSøkerIKS = fagsakRepository.finnFagsakForAktør(aktørIKS)
            if (fagsakPåSøkerIKS != null) {
                logger.info("Søker fra infotrygd finnes allerede i KS på fagsak=$fagsakPåSøkerIKS")
            }
        }

        val minimalFagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequestDto(personIdent = søkerOgBarn.søkerIdent))
        val sendBrevTask = SendInformasjonsbrevKontantstøtteendringTask.lagTask(minimalFagsak.id)
        taskService.save(sendBrevTask)
    }

    companion object {
        fun lagTask(søkerOgBarn: SøkerOgBarn): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(søkerOgBarn),
                properties =
                    Properties().apply {
                        this["fødselsnummerSøker"] = søkerOgBarn.søkerIdent
                    },
            )
        }

        const val TASK_STEP_TYPE = "opprettFagsakOgSendInformasjonsbrevKontantstøtteendringTask"
    }
}
