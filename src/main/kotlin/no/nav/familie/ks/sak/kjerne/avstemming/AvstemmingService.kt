package no.nav.familie.ks.sak.kjerne.avstemming

import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AvstemmingService(
    private val oppdragKlient: OppdragKlient,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
) {
    fun sendGrensesnittavstemming(
        fom: LocalDateTime,
        tom: LocalDateTime,
        avstemmingId: UUID?,
    ) {
        oppdragKlient.sendGrensesnittavstemmingTilOppdrag(fom = fom, tom = tom, avstemmingId = avstemmingId)
    }

    fun sendKonsistensavstemmingStartMelding(
        avstemmingstidspunkt: LocalDateTime,
        transaksjonsId: UUID,
    ) {
        logger.info("Utfører Konsistensavstemming: Sender start melding for transaksjonsId $transaksjonsId")
        oppdragKlient.konsistensavstemOppdragStart(avstemmingstidspunkt, transaksjonsId)
    }

    fun sendKonsistensavstemmingData(
        avstemmingstidspunkt: LocalDateTime,
        perioderTilAvstemming: List<PerioderForBehandling>,
        transaksjonsId: UUID,
    ) {
        logger.info("Utfører Konsistensavstemming: Sender perioder for transaksjonsId $transaksjonsId")
        oppdragKlient.konsistensavstemOppdragData(avstemmingstidspunkt, perioderTilAvstemming, transaksjonsId)
    }

    fun sendKonsistensavstemmingAvsluttMelding(
        avstemmingstidspunkt: LocalDateTime,
        transaksjonsId: UUID,
    ) {
        logger.info("Utfører Konsistensavstemming: Sender avslutt melding for transaksjonsId $transaksjonsId")
        oppdragKlient.konsistensavstemOppdragAvslutt(avstemmingstidspunkt, transaksjonsId)
    }

    fun hentDataForKonsistensavstemming(
        avstemmingtidspunkt: LocalDateTime,
        relevanteBehandlingIder: List<Long>,
    ): List<PerioderForBehandling> {
        val relevanteAndeler =
            beregningService.hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
                relevanteBehandlingIder,
                avstemmingtidspunkt,
            )
        val aktiveFødselsnummere =
            behandlingService.hentAktivtFødselsnummerForBehandlinger(
                relevanteAndeler.mapNotNull { it.kildeBehandlingId },
            )
        return relevanteAndeler
            .groupBy { it.kildeBehandlingId }
            .map { (kildeBehandlingId, andeler) ->
                if (kildeBehandlingId == null) secureLogger.warn("Finner ikke behandlingsId for andeler=$andeler")
                PerioderForBehandling(
                    behandlingId = kildeBehandlingId.toString(),
                    aktivFødselsnummer =
                        aktiveFødselsnummere[kildeBehandlingId]
                            ?: throw Feil("Finnes ikke et aktivt fødselsnummer for behandling $kildeBehandlingId"),
                    perioder =
                        andeler
                            .map {
                                it.periodeOffset ?: throw Feil("Andel $it mangler periodeOffset")
                            }.toSet(),
                )
            }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AvstemmingService::class.java)
    }
}
