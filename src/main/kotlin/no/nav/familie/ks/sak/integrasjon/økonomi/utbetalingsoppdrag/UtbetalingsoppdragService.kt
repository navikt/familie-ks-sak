package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import io.micrometer.core.instrument.Metrics
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UtbetalingsoppdragService(
    private val oppdragKlient: OppdragKlient,
    private val beregningService: BeregningService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val behandlingService: BehandlingService
) {
    private val sammeOppdragSendtKonflikt = Metrics.counter("familie.ks.sak.samme.oppdrag.sendt.konflikt")

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
        vedtak: Vedtak,
        saksbehandlerId: String,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory
    ): TilkjentYtelse {
        val oppdatertBehandling = vedtak.behandling
        val tilkjentYtelse = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            vedtak,
            saksbehandlerId,
            andelTilkjentYtelseForUtbetalingsoppdragFactory
        )
        val utbetalingsoppdrag =
            objectMapper.readValue(tilkjentYtelse.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)

        // lagre tilkjent ytelse
        val oppdatertTilkjentYtelse = beregningService.populerTilkjentYtelse(oppdatertBehandling, utbetalingsoppdrag)
        beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(oppdatertTilkjentYtelse)

        tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId = vedtak.behandling.id)
        iverksettOppdrag(utbetalingsoppdrag, oppdatertBehandling.id)

        return tilkjentYtelse
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, behandlingId: Long) {
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) {
            logger.warn("Iverksetter ikke noe mot oppdrag. Ingen utbetalingsperioder. behandlingId=$behandlingId")
            return
        }
        try {
            oppdragKlient.iverksettOppdrag(utbetalingsoppdrag)
        } catch (exception: Exception) {
            if (exception is RessursException &&
                exception.httpStatus == HttpStatus.CONFLICT
            ) {
                sammeOppdragSendtKonflikt.increment()
                logger.info("Bypasset feil med HttpKode 409 ved iverksetting mot økonomi for fagsak ${utbetalingsoppdrag.saksnummer}")
                return
            } else {
                throw exception
            }
        }
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus =
        oppdragKlient.hentStatus(oppdragId)

    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
        erSimulering: Boolean = false
    ): TilkjentYtelse {
        val behandlingId = vedtak.behandling.id
        val behandling = vedtak.behandling
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)

        // Henter tilkjentYtelse som har utbetalingsoppdrag og var sendt til oppdrag fra forrige iverksatt behandling
        val forrigeBehandlingSomErIverksatt = behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
        val forrigeTilkjentYtelseMedAndeler =
            forrigeBehandlingSomErIverksatt?.let { beregningService.hentTilkjentYtelseForBehandling(it.id) }

        val sisteOffsetPerIdent = beregningService.hentSisteOffsetPerIdent(
            behandling.fagsak.id,
            andelTilkjentYtelseForUtbetalingsoppdragFactory
        )
        val sisteOffsetPåFagsak = beregningService.hentSisteOffsetPåFagsak(behandling)

        val vedtakMedTilkjentYtelse = VedtakMedTilkjentYtelse(
            tilkjentYtelse = tilkjentYtelse,
            vedtak = vedtak,
            saksbehandlerId = saksbehandlerId,
            sisteOffsetPerIdent = sisteOffsetPerIdent,
            sisteOffsetPåFagsak = sisteOffsetPåFagsak,
            erSimulering = erSimulering
        )

        return utbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
            vedtakMedTilkjentYtelse = vedtakMedTilkjentYtelse,
            forrigeTilkjentYtelseMedAndeler = forrigeTilkjentYtelseMedAndeler,
            andelTilkjentYtelseForUtbetalingsoppdragFactory = andelTilkjentYtelseForUtbetalingsoppdragFactory
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(UtbetalingsoppdragService::class.java)
    }
}
