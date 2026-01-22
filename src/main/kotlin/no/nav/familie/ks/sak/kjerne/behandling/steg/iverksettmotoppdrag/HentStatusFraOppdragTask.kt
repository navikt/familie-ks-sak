package no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.skalIverksettesMotOppdrag
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask
import no.nav.familie.ks.sak.task.utledNesteTriggerTidIHverdagerForTask
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Properties

/**
 * Task som kjører 100 ganger før den blir satt til feilet.
 * 100 ganger tilsvarer ca 1 døgn med rekjøringsintervall 15 minutter.
 *
 *
 * Infotrygd er vanligvis stengt mellom 21 og 6, men ikke alltid.
 * Hvis tasken/steget feiler i denne tida så lager den en ny task og kjører den kl 06
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = HentStatusFraOppdragTask.TASK_STEP_TYPE,
    beskrivelse = "Henter status fra oppdrag",
    maxAntallFeil = 100,
)
class HentStatusFraOppdragTask(
    private val oppdragKlient: OppdragKlient,
    private val taskService: TaskRepositoryWrapper,
    private val stegService: StegService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val statusFraOppdragDto = objectMapper.readValue(task.payload, HentStatusFraOppdragDto::class.java)
        val oppdragId = statusFraOppdragDto.oppdragId
        logger.info("Henter status fra oppdrag for oppdragId=$oppdragId,behandlingId=${statusFraOppdragDto.behandlingsId}")

        val statusFraOppdrag = hentStatus(oppdragId = oppdragId, behandlingId = BehandlingId(statusFraOppdragDto.behandlingsId))
        logger.info("Mottok status '$statusFraOppdrag' fra oppdrag")

        when (statusFraOppdrag) {
            OppdragStatus.LAGT_PÅ_KØ -> {
                throw RekjørSenereException(
                    årsak = "Mottok ${statusFraOppdrag.name} fra oppdrag.",
                    triggerTid = utledNesteTriggerTidIHverdagerForTask(minimumForsinkelse = Duration.ofMinutes(15)),
                )
            }

            OppdragStatus.KVITTERT_OK -> {
                stegService.utførStegEtterIverksettelseAutomatisk(statusFraOppdragDto.behandlingsId)
            }

            else -> { // For andre feilene setter tasken til MANUELL_OPPFØLGING slik at det kan analyseres manuelt
                taskService.save(task.copy(status = Status.MANUELL_OPPFØLGING))
            }
        }
    }

    fun hentStatus(
        oppdragId: OppdragId,
        behandlingId: BehandlingId,
    ): OppdragStatus =
        if (tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingId.id).skalIverksettesMotOppdrag()) {
            oppdragKlient.hentStatus(oppdragId)
        } else {
            OppdragStatus.KVITTERT_OK
        }

    override fun onCompletion(task: Task) {
        val statusFraOppdragDto = objectMapper.readValue(task.payload, HentStatusFraOppdragDto::class.java)
        // lag task for sending av stønadsstatistikk
        taskService.save(PubliserVedtakTask.opprettTask(statusFraOppdragDto.personIdent, statusFraOppdragDto.behandlingsId))
    }

    companion object {
        const val TASK_STEP_TYPE = "hentStatusFraOppdrag"
        private val logger: Logger = LoggerFactory.getLogger(HentStatusFraOppdragTask::class.java)

        fun opprettTask(
            behandling: Behandling,
            vedtakId: Long,
        ): Task {
            val statusFraOppdragDto =
                HentStatusFraOppdragDto(
                    fagsystem = FAGSYSTEM,
                    personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                    behandlingsId = behandling.id,
                    vedtaksId = vedtakId,
                )
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(statusFraOppdragDto),
                properties =
                    Properties().apply {
                        this["personIdent"] = behandling.fagsak.aktør.aktivFødselsnummer()
                        this["behandlingsId"] = behandling.id.toString()
                        this["vedtakId"] = vedtakId.toString()
                    },
            )
        }
    }
}

internal data class HentStatusFraOppdragDto(
    val fagsystem: String,
    // OppdragId trenger personIdent
    val personIdent: String,
    val behandlingsId: Long,
    val vedtaksId: Long,
) {
    val oppdragId get() = OppdragId(fagsystem, personIdent, behandlingsId.toString())
}
