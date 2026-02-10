package no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.ks.infotrygd.feed.VedtakDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.infotrygd.KafkaProducer
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.lagVertikalePerioder
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVedtakHendelseTilInfotrygdTask.TASK_STEP_TYPE,
    beskrivelse = "Send vedtak hendelse til Infotrygd feed.",
)
class SendVedtakHendelseTilInfotrygdTask(
    private val kafkaProducer: KafkaProducer,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val vedtakHendelseDto = objectMapper.readValue(task.payload, VedtakHendelseDto::class.java)
        logger.info("Sender Vedtak hendelse for behandling=${vedtakHendelseDto.behandlingId} via Kafka")

        val førsteUtbetalingsdato = finnFørsteUtbetalingsdato(vedtakHendelseDto.behandlingId)
        kafkaProducer.sendVedtakHendelseTilInfotrygd(
            VedtakDto(
                datoStartNyKS = førsteUtbetalingsdato,
                fnrStoenadsmottaker = vedtakHendelseDto.fnrStoenadsmottaker,
            ),
        )
    }

    private fun finnFørsteUtbetalingsdato(behandlingId: Long): LocalDate {
        val andelerMedEndringer =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        return when {
            andelerMedEndringer.isNotEmpty() -> {
                val førsteUtbetalingsperiode =
                    andelerMedEndringer
                        .lagVertikalePerioder()
                        .tilPerioder()
                        .filtrerIkkeNull()
                        .sortedWith(compareBy({ it.fom }, { it.tom }))
                        .first()
                checkNotNull(førsteUtbetalingsperiode.fom)
            }

            else -> {
                throw Feil("Finner ikke utbetalingsperiode")
            }
        }
    }

    companion object {
        const val TASK_STEP_TYPE = "sendVedtakHendelseTilInfotrygdFeed"
        private val logger = LoggerFactory.getLogger(this::class.java)

        fun opprettTask(
            fnrStoenadsmottaker: String,
            behandlingId: Long,
        ): Task {
            logger.info("Oppretter task for å sende vedtak hendelse for behandling=$behandlingId til Infotrygd.")
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(VedtakHendelseDto(fnrStoenadsmottaker, behandlingId)),
                properties =
                    Properties().apply {
                        this["personIdent"] = fnrStoenadsmottaker
                    },
            )
        }
    }

    data class VedtakHendelseDto(
        val fnrStoenadsmottaker: String,
        val behandlingId: Long,
    )
}
